package dev.emortal.immortal.game

import dev.emortal.immortal.game.GameManager.joinGameOrNew
import dev.emortal.immortal.util.*
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import net.minestom.server.MinecraftServer
import net.minestom.server.adventure.audience.PacketGroupingAudience
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.EventNode
import net.minestom.server.event.instance.AddEntityToInstanceEvent
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.instance.Instance
import net.minestom.server.scoreboard.Sidebar
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule
import org.tinylog.kotlin.Logger
import world.cepi.kstom.Manager
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

abstract class Game : PacketGroupingAudience {

    companion object {
        private val lastGameId = AtomicInteger(0)
    }

    // Game Options
    abstract val maxPlayers: Int
    abstract val minPlayers: Int
    abstract val countdownSeconds: Int
    abstract val canJoinDuringGame: Boolean
    abstract val showScoreboard: Boolean
    abstract val showsJoinLeaveMessages: Boolean
    abstract val allowsSpectators: Boolean

    val playerCount = AtomicInteger(0)

    val id = lastGameId.getAndIncrement()

    var gameState = GameState.NOT_CREATED

    /**
     * Not guaranteed to be ran once per player
     */
    open fun getSpawnPosition(player: Player, spectator: Boolean = false) = Pos(0.5, 70.0, 0.5)

    val gameName = GameManager.getGameName(this::class)!!
    val gameTypeInfo = GameManager.getGameTypeInfo(gameName) ?: throw Error("Game type not registered")

    var instance: Instance? = null

    var startingTask: Task? = null
    var startingSecondsLeft: AtomicInteger = AtomicInteger(0)
    var scoreboard: Sidebar? = null

    val runnableGroup = RunnableGroup()

    var createFuture: CompletableFuture<Void>? = CompletableFuture()

    fun create(): CompletableFuture<Void>? {
        if (gameState != GameState.NOT_CREATED) {
            if (gameState == GameState.DESTROYED) return null
            return CompletableFuture.completedFuture(null)
        }

        Logger.info("Creating game $gameName#$id")

        try {
            val instanceCreate = instanceCreate()
            instance = instanceCreate.get(10, TimeUnit.SECONDS)
        } catch (e: Exception) {
            throw e
        }
        instance!!.setTag(GameManager.gameNameTag, gameName)
        instance!!.setTag(GameManager.gameIdTag, id)

        val eventNode = instance!!.eventNode()

        eventNode.addListener(AddEntityToInstanceEvent::class.java) {
            val player = it.entity as? Player ?: return@addListener

            // Wait until player has fully joined
            player.scheduleNextTick {
//                    if (player.instance?.uniqueId != instance.uniqueId)
                if (player.hasTag(GameManager.spectatingTag)) {
                    Logger.info("${player.username} had spectating tag")
                    addSpectator(player)
                } else {
                    Logger.info("${player.username} did not have spectating tag")
                    addPlayer(player)
                }
            }
        }

        eventNode.addListener(RemoveEntityFromInstanceEvent::class.java) {
            val player = it.entity as? Player ?: return@addListener

            playerCount.decrementAndGet()

            val hadTag = player.hasTag(GameManager.spectatingTag)
            player.removeTag(GameManager.spectatingTag)

            it.instance.scheduleNextTick { inst ->
                if (hadTag) {
                    removeSpectator(player)
                } else {
                    removePlayer(player)
                }

                if (inst.players.isNotEmpty()) return@scheduleNextTick

                end()
            }
        }

        if (showScoreboard) {
            scoreboard = Sidebar(gameTypeInfo.title)

            scoreboard?.createLine(Sidebar.ScoreboardLine("headerSpacer", Component.empty(), 99))

            scoreboard?.createLine(
                Sidebar.ScoreboardLine(
                    "infoLine",
                    Component.text()
                        .append(Component.text("Waiting for players...", NamedTextColor.GRAY))
                        .build(),
                    0
                )
            )
            scoreboard?.createLine(Sidebar.ScoreboardLine("footerSpacer", Component.empty(), -8))
            scoreboard?.createLine(
                Sidebar.ScoreboardLine(
                    "ipLine",
                    Component.text()
                        .append(Component.text("mc.emortal.dev ", NamedTextColor.DARK_GRAY))
                        .append(Component.text("       ", NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH))
                        .build(),
                    -9
                )
            )
        }

        gameState = GameState.WAITING_FOR_PLAYERS

        createFuture!!.complete(null)

        gameCreated()

        return createFuture!!
    }

    /**
     * Called when the game and instance have been registered.
     * This is when players can join.
     */
    open fun gameCreated() {}

    private fun addPlayer(player: Player, joinMessage: Boolean = showsJoinLeaveMessages) {
        Logger.info("${player.username} joining game ${gameTypeInfo.name}#$id")

        if (joinMessage) sendMessage(
            Component.text()
                .append(Component.text("JOIN", NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text(player.username, NamedTextColor.GREEN))
                .append(Component.text(" joined the game ", NamedTextColor.GRAY))
                .also {
                    if (gameState == GameState.WAITING_FOR_PLAYERS) it.append(Component.text("(${players.size}/${maxPlayers})", NamedTextColor.DARK_GRAY))
                }
        )

        val playerSpawnPoint = getSpawnPosition(player, false)

        player.respawnPoint = playerSpawnPoint

        player.reset()
        player.resetTeam()
        scoreboard?.addViewer(player)
        playSound(Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.MASTER, 1f, 1.2f))
        player.clearTitle()
        player.sendActionBar(Component.empty())

        try {
            playerJoin(player)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        refreshPlayerCount()

        if (gameState == GameState.WAITING_FOR_PLAYERS && players.size >= minPlayers) {


            if (startingTask == null) {
                startCountdown()
            } else {
                if ((maxPlayers / 2.0) < players.size) {
                    if (startingSecondsLeft.get() < 15) return
                    startingSecondsLeft.set(15)

                    sendActionBar(
                        Component.text("Time shortened to 15 seconds!", NamedTextColor.GREEN)
                    )
                    playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_XYLOPHONE, Sound.Source.MASTER, 1f, 2f), Sound.Emitter.self())
                }
            }
        }
    }

    private fun removePlayer(player: Player, leaveMessage: Boolean = showsJoinLeaveMessages) {
        scoreboard?.removeViewer(player)

        Logger.info("${player.username} leaving game ${gameTypeInfo.name}#$id")

        if (instance == null) return
        refreshPlayerCount()

        if (leaveMessage) sendMessage(
            Component.text()
                .append(Component.text("QUIT", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text(player.username, NamedTextColor.RED))
                .append(Component.text(" left the game ", NamedTextColor.GRAY))
                .also {
                    if (gameState == GameState.WAITING_FOR_PLAYERS) it.append(Component.text("(${players.size}/${maxPlayers})", NamedTextColor.DARK_GRAY))
                }
        )
        playSound(Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.MASTER, 1f, 0.5f))

        if (players.size < minPlayers) {
            if (startingTask != null) {
                cancelCountdown()
            }
        }

        if (gameState == GameState.PLAYING) {
            if (players.size == 1 && minPlayers != 1) {
                victory(players.first())
            }
        }

        try {
            playerLeave(player)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    open fun refreshPlayerCount() {
        synchronized(players) {
            JedisStorage.jedis?.publish("playercount", "$gameName ${GameManager.getGames(gameName)?.sumOf { if (it.instance == null) 0 else it.players.size } ?: 0}")

            if (instance != null && minPlayers > players.size && gameState == GameState.WAITING_FOR_PLAYERS) {
                scoreboard?.updateLineContent(
                    "infoLine",
                    Component.text(
                        "Waiting for players... (${minPlayers - players.size} more)",
                        NamedTextColor.GRAY
                    )
                )
            }
        }
    }

    internal open fun addSpectator(player: Player) {
        Logger.info("${player.username} started spectating game ${gameTypeInfo.name}#$id")

        player.reset()
        player.resetTeam()

        scoreboard?.addViewer(player)

        player.updateViewableRule { false }
        player.isInvisible = true
        player.gameMode = GameMode.SPECTATOR
        player.isAllowFlying = true
        player.isFlying = true
        //player.inventory.setItemStack(4, ItemStack.of(Material.COMPASS))
        player.playSound(Sound.sound(SoundEvent.ENTITY_BAT_AMBIENT, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self())

        try {
            spectatorJoin(player)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    internal open fun removeSpectator(player: Player) {
        Logger.info("${player.username} stopped spectating game ${gameTypeInfo.name}#$id")

        scoreboard?.removeViewer(player)

        if (instance == null) return
        try {
            spectatorLeave(player)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    open fun spectatorJoin(player: Player) {}
    open fun spectatorLeave(player: Player) {}

    abstract fun playerJoin(player: Player)
    abstract fun playerLeave(player: Player)
    abstract fun gameStarted()
    abstract fun gameEnded()

    open fun startCountdown() {
        if (countdownSeconds == 0) {
            start()
            return
        }

        startingTask = instance?.scheduler()?.submitTask {
            val count = startingSecondsLeft.incrementAndGet()

            if (count >= countdownSeconds) {
                start()
                return@submitTask TaskSchedule.stop()
            }

            scoreboard?.updateLineContent(
                "infoLine",
                Component.text()
                    .append(Component.text("Starting in ", TextColor.color(59, 128, 59)))
                    .append(Component.text(countdownSeconds - count, NamedTextColor.GREEN))
                    .append(Component.text(" seconds", TextColor.color(59, 128, 59)))
                    .build()
            )

            if ((countdownSeconds - count) < 5 || count % 5L == 0L) {
                playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.AMBIENT, 1f, 1.2f))
                showTitle(
                    Title.title(
                        Component.text(countdownSeconds - count, NamedTextColor.GREEN, TextDecoration.BOLD),
                        Component.empty(),
                        Title.Times.times(
                            Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(250)
                        )
                    )
                )
            }

            TaskSchedule.seconds(1)
        }
    }

    open fun cancelCountdown() {
        startingTask?.cancel()
        startingTask = null

        startingSecondsLeft.set(0)

        showTitle(
            Title.title(
                Component.empty(),
                Component.text("Start cancelled!", NamedTextColor.RED, TextDecoration.BOLD),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofSeconds(1))
            )
        )
        playSound(Sound.sound(SoundEvent.ENTITY_VILLAGER_NO, Sound.Source.AMBIENT, 1f, 1f))
    }

    abstract fun registerEvents(eventNode: EventNode<InstanceEvent>)

    open fun start() {
        if (gameState != GameState.WAITING_FOR_PLAYERS) return

        playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self())

        startingTask = null
        gameState = GameState.PLAYING
        scoreboard?.updateLineContent("infoLine", Component.empty())

        registerEvents(instance!!.eventNode())
        gameStarted()
    }

    open fun end() {
        if (gameState == GameState.DESTROYED) return
        gameState = GameState.DESTROYED

        Logger.info("Game ${gameTypeInfo.name}#$id is ending")

//        Manager.globalEvent.removeChild(eventNode)
//        eventNode = null

        gameEnded()



        startingTask?.cancel()
        startingTask = null
        runnableGroup.cancelAll()

        val debugMode = System.getProperty("debug").toBoolean()
        val debugGame = System.getProperty("debuggame")

        // Both spectators and players

        val joinCountDown = CountDownLatch(players.size)

        players.shuffled().iterator().forEachRemaining {
            scoreboard?.removeViewer(it)

            val future = if (debugMode) {
                it.joinGameOrNew(debugGame, hasCooldown = false)
            } else {
                it.joinGameOrNew(gameName, hasCooldown = false)
            }

            if (future == null) {
                joinCountDown.countDown()
            } else {
                future.thenRun {
                    joinCountDown.countDown()
                }
            }
        }

        // Destroy game once all players have moved to a new one
        joinCountDown.toFuture().thenRun { destroy() }
    }

    private fun destroy() {
        instance!!.scheduler().buildTask {
            refreshPlayerCount()

            createFuture = null
            startingTask?.cancel()
            startingTask = null

            GameManager.removeGame(this)
            MinecraftServer.getInstanceManager().unregisterInstance(instance!!)
            instance = null
        }.delay(TaskSchedule.seconds(2)).schedule()
    }

    open fun canBeJoined(player: Player): Boolean {
        if (playerCount.get() >= maxPlayers) return false
        if (gameState == GameState.PLAYING) return canJoinDuringGame
        return gameState.joinable
    }

    fun victory(team: Team) = victory(team.players)
    fun victory(player: Player) = victory(listOf(player))

    open fun victory(winningPlayers: Collection<Player>) {
        if (gameState == GameState.ENDING) return
        gameState = GameState.ENDING



        val victorySound = Sound.sound(SoundEvent.ENTITY_VILLAGER_CELEBRATE, Sound.Source.MASTER, 1f, 1f)
        val victorySound2 = Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER, 1f, 1f)

        val defeatSound = Sound.sound(SoundEvent.ENTITY_VILLAGER_DEATH, Sound.Source.MASTER, 1f, 0.8f)

        players.forEach {
            val victoryTitle = Title.title(
                Component.text("VICTORY!", NamedTextColor.GOLD, TextDecoration.BOLD),
                Component.text(EndGameQuotes.getVictoryQuote(it), NamedTextColor.GRAY),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(3))
            )
            val defeatTitle = Title.title(
                Component.text("DEFEAT!", NamedTextColor.RED, TextDecoration.BOLD),
                Component.text(EndGameQuotes.getDefeatQuote(it), NamedTextColor.GRAY),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(3))
            )

            if (winningPlayers.contains(it)) {
                it.showTitle(victoryTitle)
                it.playSound(victorySound)
                it.playSound(victorySound2)
            } else {
                it.showTitle(defeatTitle)
                it.playSound(defeatSound)
            }
        }

        gameWon(winningPlayers)

        Manager.scheduler.buildTask { end() }.delay(TaskSchedule.seconds(6)).schedule()
    }

    open fun gameWon(winningPlayers: Collection<Player>) {}

    abstract fun instanceCreate(): CompletableFuture<Instance>

    override fun getPlayers(): MutableCollection<Player> = instance!!.players

    override fun equals(other: Any?): Boolean {
        if (other !is Game) return false
        return other.id == this.id
    }

}
