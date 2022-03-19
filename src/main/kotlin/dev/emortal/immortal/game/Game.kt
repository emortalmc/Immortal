package dev.emortal.immortal.game

import dev.emortal.acquaintance.RelationshipManager.party
import dev.emortal.immortal.config.GameOptions
import dev.emortal.immortal.event.GameDestroyEvent
import dev.emortal.immortal.event.PlayerJoinGameEvent
import dev.emortal.immortal.event.PlayerLeaveGameEvent
import dev.emortal.immortal.game.GameManager.gameIdTag
import dev.emortal.immortal.game.GameManager.gameNameTag
import dev.emortal.immortal.game.GameManager.joinGameOrNew
import dev.emortal.immortal.util.MinestomRunnable
import dev.emortal.immortal.util.reset
import dev.emortal.immortal.util.resetTeam
import dev.emortal.immortal.util.safeSetInstance
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import net.minestom.server.adventure.audience.PacketGroupingAudience
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.EventDispatcher
import net.minestom.server.event.EventFilter
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerEntityInteractEvent
import net.minestom.server.event.player.PlayerStartSneakingEvent
import net.minestom.server.event.trait.PlayerEvent
import net.minestom.server.instance.Instance
import net.minestom.server.scoreboard.Sidebar
import net.minestom.server.sound.SoundEvent
import org.slf4j.LoggerFactory
import world.cepi.kstom.Manager
import world.cepi.kstom.event.listenOnly
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

abstract class Game(val gameOptions: GameOptions) : PacketGroupingAudience {

    val players: MutableSet<Player> = ConcurrentHashMap.newKeySet()
    val spectators: MutableSet<Player> = ConcurrentHashMap.newKeySet()
    val teams = mutableSetOf<Team>()

    var gameState = GameState.WAITING_FOR_PLAYERS

    val gameTypeInfo = GameManager.registeredGameMap[GameManager.registeredClassMap[this::class]] ?: throw Error("Game type not registered")
    val id = GameManager.nextGameID

    val logger = LoggerFactory.getLogger(gameTypeInfo.name)

    open var spawnPosition = Pos(0.5, 70.0, 0.5)

    val instance: Instance = instanceCreate().also {
        it.setTag(gameNameTag, gameTypeInfo.name)
        it.setTag(gameIdTag, id)
    }

    val eventNode = EventNode.type(
        "${gameTypeInfo.name}-$id",
        EventFilter.INSTANCE
    ) { a, b ->
        if (a is PlayerEvent) {
            return@type players.contains(a.player)
        } else {
            return@type b.getTag(gameIdTag) == id
        }
    }

    val spectatorNode = EventNode.type(
        "${gameTypeInfo.name}-$id-spectator",
        EventFilter.INSTANCE
    ) { a, b ->
        if (a is PlayerEvent) {
            return@type spectators.contains(a.player)
        } else {
            return@type b.getTag(gameIdTag) == id
        }
    }

    val timer = Timer()

    var startingTask: MinestomRunnable? = null
    var scoreboard: Sidebar? = null

    //val spectatorGUI = SpectatingGUI()

    private var destroyed = false
    var gameCreator: Player? = null

    init {
        Manager.globalEvent.addChild(eventNode)
        Manager.globalEvent.addChild(spectatorNode)

        //eventNode.listenOnly<PlayerUseItemEvent> {
        //    if (!spectators.contains(player)) return@listenOnly
        //    if (player.itemInMainHand.material != Material.COMPASS) return@listenOnly
        //    player.openInventory(spectatorGUI.inventory)
        //}
        spectatorNode.listenOnly<PlayerStartSneakingEvent> {
            if (!spectators.contains(player)) return@listenOnly
            player.stopSpectating()
        }
        spectatorNode.listenOnly<PlayerEntityInteractEvent> {
            if (!spectators.contains(player)) return@listenOnly
            val playerToSpectate = entity as? Player ?: return@listenOnly
            player.spectate(playerToSpectate)
        }

        if (gameTypeInfo.whenToRegisterEvents == WhenToRegisterEvents.IMMEDIATELY) registerEvents()

        if (gameOptions.showScoreboard) {
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

        logger.info("A game of '${gameTypeInfo.name}' was created")
    }

    @Synchronized internal fun addPlayer(player: Player, joinMessage: Boolean = gameOptions.showsJoinLeaveMessages): CompletableFuture<Boolean> {
        if (players.contains(player)) return CompletableFuture.completedFuture(false)

        logger.info("${player.username} joining game '${gameTypeInfo.name}'")

        players.add(player)
        //spectatorGUI.refresh(players)

        if (gameOptions.minPlayers > players.size) {
            scoreboard?.updateLineContent(
                "infoLine",
                Component.text(
                    "Waiting for players... (${gameOptions.minPlayers - players.size} more)",
                    NamedTextColor.GRAY
                )
            )
        }

        player.respawnPoint = spawnPosition

        val future = CompletableFuture<Boolean>()
        player.safeSetInstance(instance, spawnPosition).thenRun {
            player.reset()
            player.resetTeam()

            scoreboard?.addViewer(player)

            if (joinMessage) sendMessage(
                Component.text()
                    .append(Component.text("JOIN", NamedTextColor.GREEN, TextDecoration.BOLD))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(player.username, NamedTextColor.GREEN))
                    .append(Component.text(" joined the game ", NamedTextColor.GRAY))
                    .also {
                        if (gameState == GameState.WAITING_FOR_PLAYERS) it.append(Component.text("(${players.size}/${gameOptions.maxPlayers})", NamedTextColor.DARK_GRAY))
                    }
            )
            playSound(Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.MASTER, 1f, 1.2f))
            player.playSound(Sound.sound(SoundEvent.ENTITY_ENDERMAN_TELEPORT, Sound.Source.MASTER, 1f, 1f))
            player.clearTitle()
            player.sendActionBar(Component.empty())

            val joinEvent = PlayerJoinGameEvent(this, player)
            EventDispatcher.call(joinEvent)

            playerJoin(player)

            if (gameState == GameState.WAITING_FOR_PLAYERS && players.size >= gameOptions.minPlayers) {
                if (startingTask == null) {
                    startCountdown()
                }
            }

            future.complete(true)
        }

        return future
    }

    @Synchronized internal fun removePlayer(player: Player, leaveMessage: Boolean = gameOptions.showsJoinLeaveMessages) {
        if (!players.contains(player)) return

        logger.info("${player.username} leaving game '${gameTypeInfo.name}'")

        teams.forEach {
            it.remove(player)
        }
        players.remove(player)

        if (gameOptions.minPlayers > players.size) {
            scoreboard?.updateLineContent(
                "infoLine",
                Component.text(
                    "Waiting for players... (${gameOptions.minPlayers - players.size} more)",
                    NamedTextColor.GRAY
                )
            )
        }

        //spectatorGUI.refresh(players)
        scoreboard?.removeViewer(player)

        val leaveEvent = PlayerLeaveGameEvent(this, player)
        EventDispatcher.call(leaveEvent)

        if (leaveMessage) sendMessage(
            Component.text()
                .append(Component.text("QUIT", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text(player.username, NamedTextColor.RED))
                .append(Component.text(" left the game ", NamedTextColor.GRAY))
                .also {
                    if (gameState == GameState.WAITING_FOR_PLAYERS) it.append(Component.text("(${players.size}/${gameOptions.maxPlayers})", NamedTextColor.DARK_GRAY))
                }
        )
        playSound(Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.MASTER, 1f, 0.5f))

        if (players.size < gameOptions.minPlayers) {
            if (startingTask != null) {
                cancelCountdown()
            }
        }

        if (gameState == GameState.PLAYING) {
            val teamsWithPlayers = teams.filter { it.players.isNotEmpty() }
            if (teamsWithPlayers.size == 1) {
                victory(teamsWithPlayers.first())
            }
            if (players.size == 1) {
                victory(players.first())
            }
        }

        if (players.size == 0) {
            destroy()
            return
        }

        playerLeave(player)
    }

    @Synchronized internal fun addSpectator(player: Player): CompletableFuture<Boolean> {
        if (spectators.contains(player)) return CompletableFuture.completedFuture(false)
        if (players.contains(player)) return CompletableFuture.completedFuture(false)

        logger.info("${player.username} started spectating game '${gameTypeInfo.name}'")

        player.respawnPoint = spawnPosition

        val future = CompletableFuture<Boolean>()
        player.safeSetInstance(instance).thenRun {
            player.reset()

            scoreboard?.addViewer(player)

            player.isInvisible = true
            player.gameMode = GameMode.SPECTATOR
            player.isAllowFlying = true
            player.isFlying = true
            //player.inventory.setItemStack(4, ItemStack.of(Material.COMPASS))
            player.playSound(Sound.sound(SoundEvent.ENTITY_BAT_AMBIENT, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self())

            spectatorJoin(player)

            future.complete(true)
        }

        spectators.add(player)

        return future
    }

    @Synchronized internal fun removeSpectator(player: Player) {
        if (!spectators.contains(player)) return

        logger.info("${player.username} stopped spectating game '${gameTypeInfo.name}'")

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

    private fun startCountdown() {
        if (gameOptions.countdownSeconds == 0) {
            start()
            return
        }

        startingTask = object : MinestomRunnable(timer = timer, repeat = Duration.ofSeconds(1), iterations = gameOptions.countdownSeconds) {

            override fun run() {
                scoreboard?.updateLineContent(
                    "infoLine",
                    Component.text()
                        .append(Component.text("Starting in ${gameOptions.countdownSeconds - currentIteration} seconds", NamedTextColor.GREEN))
                        .build()
                )

                if ((gameOptions.countdownSeconds - currentIteration) < 5 || currentIteration % 5 == 0) {
                    playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.AMBIENT, 1f, 1.2f))
                    showTitle(
                        Title.title(
                            Component.text(gameOptions.countdownSeconds - currentIteration, NamedTextColor.GREEN, TextDecoration.BOLD),
                            Component.empty(),
                            Title.Times.of(
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

    fun cancelCountdown() {
        startingTask?.cancel()
        startingTask = null

        showTitle(
            Title.title(
                Component.empty(),
                Component.text("Start cancelled!", NamedTextColor.RED, TextDecoration.BOLD),
                Title.Times.of(Duration.ZERO, Duration.ofSeconds(2), Duration.ofSeconds(1))
            )
        )
        playSound(Sound.sound(SoundEvent.ENTITY_VILLAGER_NO, Sound.Source.AMBIENT, 1f, 1f))
    }

    abstract fun registerEvents()

    fun start() {
        if (gameState == GameState.PLAYING) return

        playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self())

        startingTask = null
        gameState = GameState.PLAYING
        scoreboard?.updateLineContent("infoLine", Component.empty())

        if (gameTypeInfo.whenToRegisterEvents == WhenToRegisterEvents.GAME_START) registerEvents()
        gameStarted()
    }

    fun destroy() {
        if (destroyed) return
        destroyed = true

        logger.info("A game of '${gameTypeInfo.name}' is ending")

        Manager.globalEvent.removeChild(eventNode)

        timer.cancel()

        gameDestroyed()

        val destroyEvent = GameDestroyEvent(this)
        EventDispatcher.call(destroyEvent)

        GameManager.gameMap[gameTypeInfo.name]?.remove(this)

        teams.forEach {
            it.destroy()
        }

        // Both spectators and players
        getPlayers().forEach {
            scoreboard?.removeViewer(it)
            it.joinGameOrNew("lobby")
        }
        players.clear()
        spectators.clear()
    }

    open fun canBeJoined(player: Player): Boolean {
        if (players.contains(player)) return false
        if (players.size >= gameOptions.maxPlayers) {
            return false
        }
        if (gameState == GameState.PLAYING) {
            return gameOptions.canJoinDuringGame
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

    fun victory(team: Team) {
//        sendMessage(
//            Component.text()
//                .append(Component.text("${" ".repeat(25)} VICTORY", NamedTextColor.GOLD, TextDecoration.BOLD))
//                .append(Component.text("\n${team.teamName} won the game!\n", NamedTextColor.GRAY))
//                .armify()
//        )

        victory(team.players)
    }
    fun victory(player: Player) {
//        sendMessage(
//            Component.text()
//                .append(Component.text("${" ".repeat(25)} VICTORY", NamedTextColor.GOLD, TextDecoration.BOLD))
//                .append(Component.text("\n${player.username} won the game!\n", NamedTextColor.GRAY))
//                .armify()
//        )

        victory(listOf(player))
    }

    open fun victory(winningPlayers: Collection<Player>) {
        gameState = GameState.ENDING

        val victoryTitle = Title.title(
            Component.text("VICTORY!", NamedTextColor.GOLD, TextDecoration.BOLD),
            Component.text(EndGameQuotes.victory.random(), NamedTextColor.GRAY),
            Title.Times.of(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(3))
        )
        val defeatTitle = Title.title(
            Component.text("DEFEAT!", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(EndGameQuotes.defeat.random(), NamedTextColor.GRAY),
            Title.Times.of(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(3))
        )

        val victorySound = Sound.sound(SoundEvent.ENTITY_VILLAGER_CELEBRATE, Sound.Source.MASTER, 1f, 1f)
        val victorySound2 = Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER, 1f, 1f)

        val defeatSound = Sound.sound(SoundEvent.ENTITY_VILLAGER_DEATH, Sound.Source.MASTER, 1f, 0.8f)

        players.forEach {
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

    abstract fun instanceCreate(): Instance

    override fun getPlayers(): MutableCollection<Player> = (players + spectators).toMutableSet()

}
