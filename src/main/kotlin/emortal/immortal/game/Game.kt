package emortal.immortal.game

import emortal.immortal.game.GameManager.joinGameOrNew
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.EventNode
import net.minestom.server.scoreboard.Sidebar
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.Task
import world.cepi.kstom.adventure.sendMiniMessage
import world.cepi.kstom.util.MinestomRunnable
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

abstract class Game(val gameOptions: GameOptions) {
    val players: ConcurrentHashMap.KeySetView<Player, Boolean> = ConcurrentHashMap.newKeySet()
    val playerAudience = Audience.audience(players)
    var gameState = GameState.WAITING_FOR_PLAYERS
    val gameTypeInfo = GameManager.registeredGameMap[this::class] ?: throw Error("Game type not initialized")

    val firstInstance get() = gameOptions.instances.first()

    var startingTask: Task? = null

    val childEventNode = gameTypeInfo.eventNode.addChild(EventNode.all("${gameTypeInfo.gameName}${GameManager.nextGameID}"))
    val scoreboard = Sidebar(gameTypeInfo.sidebarTitle)

    init {
        scoreboard.createLine(Sidebar.ScoreboardLine("header", Component.empty(), 30))
        scoreboard.createLine(Sidebar.ScoreboardLine("footer", Component.empty(), -1))
        scoreboard.createLine(
            Sidebar.ScoreboardLine(
                "ipLine",
                Component.text()
                    .append(Component.text("mc.emortal.dev ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("       ", NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH))
                    .build(),
                -2
            )
        )

        registerEvents()
    }

    fun addPlayer(player: Player) {
        println("${player.name} joining game ${gameTypeInfo.gameName}")
        players.add(player)
        scoreboard.addViewer(player)
        GameManager.playerGameMap[player] = this

        player.gameMode = GameMode.SPECTATOR

        playerAudience.sendMiniMessage(" <gray>[<green><bold>+</bold></green>]</gray> ${player.username} <green>joined</green>")

        playerJoin(player)

        if (gameState == GameState.PLAYING) {
            respawn(player)
            return
        }

        if (players.size == gameOptions.maxPlayers) {
            start()
            return
        }

        if (players.size >= gameOptions.playersToStart) {
            if (startingTask != null) return

            gameState = GameState.STARTING

            startingTask = object : MinestomRunnable() {
                var secs = 5

                override fun run() {
                    if (secs < 1) {
                        cancel()
                        start()
                        return
                    }

                    playerAudience.playSound(Sound.sound(SoundEvent.BLOCK_WOODEN_BUTTON_CLICK_ON, Sound.Source.BLOCK, 1f, 1f))
                    playerAudience.showTitle(
                        Title.title(
                            Component.text(secs, NamedTextColor.GREEN, TextDecoration.BOLD),
                            Component.empty(),
                            Title.Times.of(
                                Duration.ZERO, Duration.ofSeconds(1), Duration.ofMillis(250)
                            )
                        )
                    )

                    secs--
                }
            }.repeat(Duration.ofSeconds(1)).schedule()
        }
    }

    fun removePlayer(player: Player) {
        println("${player.name} leaving game ${gameTypeInfo.gameName}")
        players.remove(player)
        GameManager.playerGameMap.remove(player)
        scoreboard.removeViewer(player)

        playerAudience.sendMiniMessage(" <gray>[<red><bold>-</bold></red>]</gray> ${player.username} <red>left</red>")

        playerLeave(player)
    }

    abstract fun playerJoin(player: Player)
    abstract fun playerLeave(player: Player)

    abstract fun respawn(player: Player)

    abstract fun start()

    fun destroy() {
        gameTypeInfo.eventNode.removeChild(childEventNode)

        GameManager.gameMap[this::class]!!.remove(this)

        players.forEach {
            scoreboard.removeViewer(it)
            it.joinGameOrNew(this::class, GameManager.registeredGameMap[this::class]!!.defaultGameOptions)
        }
        players.clear()

        postDestroy()
    }
    abstract fun postDestroy()

    abstract fun registerEvents()

    open fun canBeJoined(): Boolean {
        if (gameState == GameState.PLAYING) {
            return gameOptions.joinableMidGame
        }
        return gameState.joinable
    }

}