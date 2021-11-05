package dev.emortal.immortal.game

import dev.emortal.immortal.game.GameManager.gameIdTag
import dev.emortal.immortal.game.GameManager.gameNameTag
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import net.minestom.server.adventure.audience.PacketGroupingAudience
import net.minestom.server.entity.Player
import net.minestom.server.event.EventFilter
import net.minestom.server.event.EventNode
import net.minestom.server.instance.Instance
import net.minestom.server.scoreboard.Sidebar
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.Task
import org.slf4j.LoggerFactory
import world.cepi.kstom.adventure.sendMiniMessage
import world.cepi.kstom.util.MinestomRunnable
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

abstract class Game(val gameOptions: GameOptions) : PacketGroupingAudience {

    val players: MutableSet<Player> = ConcurrentHashMap.newKeySet()
    val teams = mutableSetOf<Team>()

    var gameState = GameState.WAITING_FOR_PLAYERS
    val gameTypeInfo = GameManager.registeredGameMap[this::class] ?: throw Error("Game type not registered")
    val id = GameManager.nextGameID

    val LOGGER = LoggerFactory.getLogger("Game-${gameTypeInfo.gameName}")

    val instance: Instance = instanceCreate().also {
        it.setTag(gameNameTag, gameTypeInfo.gameName)
        it.setTag(gameIdTag, id)
    }

    val eventNode = gameTypeInfo.eventNode.addChild(EventNode.value("${gameTypeInfo.gameName}-$id", EventFilter.INSTANCE) { instance == it })

    var startingTask: Task? = null
    var scoreboard: Sidebar? = null

    init {
        LOGGER.info("A game of '${gameTypeInfo.gameName}' was created")

        if (gameOptions.showScoreboard) {
            scoreboard = gameTypeInfo.sidebarTitle?.let { Sidebar(it) }

            scoreboard?.createLine(Sidebar.ScoreboardLine("headerSpacer", Component.empty(), 30))

            scoreboard?.createLine(
                Sidebar.ScoreboardLine(
                    "InfoLine",
                    Component.text()
                        .append(Component.text("Waiting for players...", NamedTextColor.GRAY))
                        .build(),
                    0
                )
            )
            scoreboard?.createLine(Sidebar.ScoreboardLine("footerSpacer", Component.empty(), -1))
            scoreboard?.createLine(
                Sidebar.ScoreboardLine(
                    "ipLine",
                    Component.text()
                        .append(Component.text(" mc.emortal.dev ", NamedTextColor.DARK_GRAY))
                        .append(Component.text("       ", NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH))
                        .build(),
                    -2
                )
            )
        }
    }

    fun addPlayer(player: Player) {
        LOGGER.info("${player.username} joining game '${gameTypeInfo.gameName}'")

        players.add(player)
        scoreboard?.addViewer(player)
        GameManager.playerGameMap[player] = this

        player.inventory.clear()
        player.scheduleNextTick {
            if (player.instance!! != instance) player.setInstance(instance)
        }

        if (gameOptions.showsJoinLeaveMessages) sendMiniMessage("<green><bold>JOIN</bold></green> <dark_gray>|</dark_gray> ${player.username}")

        playerJoin(player)

        if (players.size >= gameOptions.minPlayers) {
            if (startingTask != null) return

            startCountdown()
        }
    }

    fun removePlayer(player: Player) {
        LOGGER.info("${player.username} leaving game '${gameTypeInfo.gameName}'")

        teams.forEach {
            it.remove(player)
        }

        players.remove(player)
        GameManager.playerGameMap.remove(player)
        scoreboard?.removeViewer(player)

        player.inventory.clear()

        if (gameOptions.showsJoinLeaveMessages) sendMiniMessage("<red><bold>QUIT</bold></red> <dark_gray>|</dark_gray> ${player.username}")

        if (players.size < gameOptions.minPlayers) {
            if (startingTask != null) {
                cancelCountdown()
            }
        }

        if (players.size == 0) {
            destroy()
            return
        }

        playerLeave(player)
    }

    abstract fun playerJoin(player: Player)
    abstract fun playerLeave(player: Player)
    abstract fun gameStarted()
    abstract fun gameDestroyed()

    fun startCountdown() {
        if (gameOptions.countdownSeconds == 0) {
            start()
            return
        }

        scoreboard?.updateLineContent("InfoLine", Component.text("Starting...", NamedTextColor.GRAY))

        startingTask = object : MinestomRunnable() {
            var secs = gameOptions.countdownSeconds

            override fun run() {
                if (secs < 1) {
                    cancel()
                    start()
                    return
                }

                playSound(Sound.sound(SoundEvent.BLOCK_WOODEN_BUTTON_CLICK_ON, Sound.Source.AMBIENT, 1f, 1f))
                showTitle(
                    Title.title(
                        Component.text(secs, NamedTextColor.GREEN, TextDecoration.BOLD),
                        Component.empty(),
                        Title.Times.of(
                            Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(250)
                        )
                    )
                )

                secs--
            }
        }.repeat(Duration.ofSeconds(1)).schedule()
    }

    fun cancelCountdown() {
        scoreboard?.updateLineContent("InfoLine", Component.text("Waiting for players...", NamedTextColor.GRAY))
        startingTask?.cancel()
        startingTask = null
        showTitle(Title.title(Component.text("Start cancelled!", NamedTextColor.RED, TextDecoration.BOLD), Component.text("Not enough players"), Title.Times.of(Duration.ZERO, Duration.ofSeconds(2), Duration.ofSeconds(1))))
        playSound(Sound.sound(SoundEvent.ENTITY_VILLAGER_NO, Sound.Source.AMBIENT, 1f, 1f))
    }

    abstract fun registerEvents()

    fun start() {
        startingTask?.cancel()
        startingTask = null
        gameState = GameState.PLAYING
        scoreboard?.updateLineContent("InfoLine", Component.empty())

        registerEvents()
        gameStarted()
    }

    fun destroy() {
        LOGGER.info("A game of '${gameTypeInfo.gameName}' was destroyed")

        gameTypeInfo.eventNode.removeChild(eventNode)

        GameManager.gameMap[this::class]!!.remove(this)

        teams.forEach {
            it.destroy()
        }

        players.forEach {
            scoreboard?.removeViewer(it)
            it.chat("/play lobby")
            // TODO: Rejoining (gameOptions.autoRejoin) it.joinGameOrNew(this::class)
        }

        gameDestroyed()
    }

    open fun canBeJoined(): Boolean {
        if (players.size >= gameOptions.maxPlayers) {
            return false
        }
        if (gameState == GameState.PLAYING) {
            return gameOptions.canJoinDuringGame
        }
        return gameState.joinable
    }

    fun registerTeam(team: Team): Team {
        teams.add(team)
        return team
    }

    abstract fun instanceCreate(): Instance

    override fun getPlayers(): MutableCollection<Player> = players

}
