package dev.emortal.immortal.game

import dev.emortal.immortal.game.GameManager.joinGameOrNew
import dev.emortal.immortal.util.*
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import net.minestom.server.adventure.audience.PacketGroupingAudience
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.EventNode
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.instance.Instance
import net.minestom.server.scoreboard.Sidebar
import net.minestom.server.sound.SoundEvent
import org.tinylog.kotlin.Logger
import world.cepi.kstom.Manager
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

abstract class Game() : PacketGroupingAudience {

    companion object {
        val joinLock = Object()
        val findLock = Object()

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


    val players = CopyOnWriteArraySet<Player>()
    val queuedPlayers = CopyOnWriteArraySet<UUID>()
    val spectators = CopyOnWriteArraySet<Player>()
    val teams = CopyOnWriteArraySet<Team>()

    val id = lastGameId.getAndIncrement()

    var gameState = GameState.NOT_CREATED

    /**
     * Not guaranteed to be ran once per player
     */
    open fun getSpawnPosition(player: Player, spectator: Boolean = false) = Pos(0.5, 70.0, 0.5)

    val gameName = GameManager.registeredClassMap[this::class]!!
    val gameTypeInfo = GameManager.registeredGameMap[gameName] ?: throw Error("Game type not registered")

    lateinit var instance: Instance

    var startingTask: MinestomRunnable? = null
    var scoreboard: Sidebar? = null

    val runnableGroup = RunnableGroup()

    var createFuture: CompletableFuture<Void>? = null

    fun create(): CompletableFuture<Void>? {
        if (gameState != GameState.NOT_CREATED) {
            if (gameState == GameState.DESTROYED) return null
            return CompletableFuture.completedFuture(null)
        }

        createFuture = CompletableFuture.runAsync({
            Logger.info("Creating game $gameName#$id")

            try {
                instance = instanceCreate().get(10, TimeUnit.SECONDS)
            } catch (e: Exception) {
                throw e
            }
            instance.setTag(GameManager.gameNameTag, gameName)
            instance.setTag(GameManager.gameIdTag, id)

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

            gameCreated()
        }, ForkJoinPool.commonPool())

        return createFuture!!
    }

    open fun gameCreated() {}

    internal open fun addPlayer(player: Player, joinMessage: Boolean = showsJoinLeaveMessages) {
        queuedPlayers.remove(player.uuid)
        if (!players.add(player)) {
            Logger.warn("Already contains player")
            return
        }

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

        player.scheduleNextTick {
            if (!player.isOnline) {
                removePlayer(player)
                return@scheduleNextTick
            }

            player.safeSetInstance(instance, playerSpawnPoint).thenRun {
                player.reset()
                player.resetTeam()
                scoreboard?.addViewer(player)
                playSound(Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.MASTER, 1f, 1.2f))
                player.clearTitle()
                player.sendActionBar(Component.empty())

                playerJoin(player)

                refreshPlayerCount()

                if (gameState == GameState.WAITING_FOR_PLAYERS && players.size >= minPlayers) {
                    if (startingTask == null) {
                        startCountdown()
                    }
                }
            }
        }
    }

    internal open fun removePlayer(player: Player, leaveMessage: Boolean = showsJoinLeaveMessages) {
        queuedPlayers.remove(player.uuid)
        players.remove(player)
//        if (!players.remove(player)) {
////            Logger.error("Player is already not in the game, leaving anyway")
////            return
//        }

        teams.forEach { it.remove(player) }

        scoreboard?.removeViewer(player)

        Logger.info("${player.username} leaving game ${gameTypeInfo.name}#$id")

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
            val teamsWithPlayers = teams.filter { it.players.isNotEmpty() }
            if (teamsWithPlayers.size == 1) {
                victory(teamsWithPlayers.first())
                playerLeave(player)
                return
            }
            if (players.size == 1) {
                victory(players.first())
            }
        }

        if (players.size == 0) {
            destroy()
        }


        playerLeave(player)
    }

    @Synchronized
    open fun refreshPlayerCount() {
        JedisStorage.jedis?.publish("playercount", "$gameName ${GameManager.getGames(gameName)?.sumOf { it.players.size } ?: 0}")

        if (minPlayers > players.size && gameState == GameState.WAITING_FOR_PLAYERS) {
            scoreboard?.updateLineContent(
                "infoLine",
                Component.text(
                    "Waiting for players... (${minPlayers - players.size} more)",
                    NamedTextColor.GRAY
                )
            )
        }
    }

    internal open fun addSpectator(player: Player) {
        if (spectators.contains(player)) return
        if (players.contains(player)) return

        Logger.info("${player.username} started spectating game ${gameTypeInfo.name}#$id")

        spectators.add(player)

            val playerSpawnPoint = getSpawnPosition(player, true)

            player.respawnPoint = playerSpawnPoint

            player.safeSetInstance(instance, playerSpawnPoint).thenRun {
                player.reset()
                player.resetTeam()

                scoreboard?.addViewer(player)

                player.isAutoViewable = false
                player.isInvisible = true
                player.gameMode = GameMode.SPECTATOR
                player.isAllowFlying = true
                player.isFlying = true
                //player.inventory.setItemStack(4, ItemStack.of(Material.COMPASS))
                player.playSound(Sound.sound(SoundEvent.ENTITY_BAT_AMBIENT, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self())

                spectatorJoin(player)
            }
    }

    internal open fun removeSpectator(player: Player) {
        if (!spectators.contains(player)) return

        Logger.info("${player.username} stopped spectating game ${gameTypeInfo.name}#$id")

        spectators.remove(player)
        scoreboard?.removeViewer(player)

        spectatorLeave(player)
    }

    open fun spectatorJoin(player: Player) {}
    open fun spectatorLeave(player: Player) {}

    abstract fun playerJoin(player: Player)
    abstract fun playerLeave(player: Player)
    abstract fun gameStarted()
    abstract fun gameDestroyed()

    open fun startCountdown() {
        if (countdownSeconds == 0) {
            start()
            return
        }

        startingTask = object : MinestomRunnable(repeat = Duration.ofSeconds(1), iterations = countdownSeconds, group = runnableGroup) {

            override fun run() {
                scoreboard?.updateLineContent(
                    "infoLine",
                    Component.text()
                        .append(Component.text("Starting in ", TextColor.color(59, 128, 59)))
                        .append(Component.text(countdownSeconds - currentIteration.get(), NamedTextColor.GREEN))
                        .append(Component.text(" seconds", TextColor.color(59, 128, 59)))
                        .build()
                )

                if ((countdownSeconds - currentIteration.get()) < 5 || currentIteration.get() % 5L == 0L) {
                    playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.AMBIENT, 1f, 1.2f))
                    showTitle(
                        Title.title(
                            Component.text(countdownSeconds - currentIteration.get(), NamedTextColor.GREEN, TextDecoration.BOLD),
                            Component.empty(),
                            Title.Times.times(
                                Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(250)
                            )
                        )
                    )
                }
            }

            override fun cancelled() {
                start()
            }

        }
    }

    open fun cancelCountdown() {
        startingTask?.cancel()
        startingTask = null

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

        registerEvents(instance.eventNode())
        gameStarted()
    }

    open fun destroy() {
        if (gameState == GameState.DESTROYED) return
        gameState = GameState.DESTROYED

        createFuture = null
        scoreboard = null

        Logger.info("Game ${gameTypeInfo.name}#$id is ending")

//        Manager.globalEvent.removeChild(eventNode)
//        eventNode = null

        gameDestroyed()

        GameManager.removeGame(this)

        teams.forEach {
            it.destroy()
        }
        startingTask?.cancelImmediate()
        startingTask = null
        runnableGroup.cancelAll()

        val debugMode = System.getProperty("debug").toBoolean()
        val debugGame = System.getProperty("debuggame")

        // Both spectators and players
        getPlayers().shuffled().forEach {
            scoreboard?.removeViewer(it)

            if (debugMode) {
                it.joinGameOrNew(debugGame, ignoreCooldown = true)
            } else {
                it.joinGameOrNew(gameName, ignoreCooldown = true)
            }

        }

        if (instance.players.isEmpty()) {
//            queuedPlayers.forEach { it.kick("Game ended") }
            Manager.instance.unregisterInstance(instance)
        }

        players.clear()
        spectators.clear()
        queuedPlayers.clear()
        teams.clear()

        refreshPlayerCount()
    }

    open fun canBeJoined(player: Player): Boolean {
        if (players.contains(player)) return false
        if (!queuedPlayers.contains(player.uuid) && players.size + queuedPlayers.size >= maxPlayers) {
            return false
        }
        if (gameState == GameState.PLAYING) {
            return canJoinDuringGame
        }
        //if (gameOptions.private) {
        //    val party = gameCreator?.party ?: return false
        //
        //    return party.players.contains(player)
        //}
        return gameState.joinable
    }

    fun registerTeam(team: Team): Team {
        teams.add(team)
        return team
    }

    fun victory(team: Team) = victory(team.players)
    fun victory(player: Player) = victory(listOf(player))

    open fun victory(winningPlayers: Collection<Player>) {
        if (gameState == GameState.ENDING) return
        gameState = GameState.ENDING

        val victoryTitle = Title.title(
            Component.text("VICTORY!", NamedTextColor.GOLD, TextDecoration.BOLD),
            Component.text(EndGameQuotes.victory.random(), NamedTextColor.GRAY),
            Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(3))
        )
        val defeatTitle = Title.title(
            Component.text("DEFEAT!", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(EndGameQuotes.defeat.random(), NamedTextColor.GRAY),
            Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(3))
        )

        val victorySound = Sound.sound(SoundEvent.ENTITY_VILLAGER_CELEBRATE, Sound.Source.MASTER, 1f, 1f)
        val victorySound2 = Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER, 1f, 1f)

        val defeatSound = Sound.sound(SoundEvent.ENTITY_VILLAGER_DEATH, Sound.Source.MASTER, 1f, 0.8f)

        getPlayers().forEach {
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

        Manager.scheduler.buildTask { destroy() }.delay(Duration.ofSeconds(6)).schedule()
    }

    open fun gameWon(winningPlayers: Collection<Player>) {}

    abstract fun instanceCreate(): CompletableFuture<Instance>

    override fun getPlayers(): MutableCollection<Player> = (players + spectators).toMutableSet()

    override fun equals(other: Any?): Boolean {
        if (other !is Game) return false
        return other.id == this.id
    }

}
