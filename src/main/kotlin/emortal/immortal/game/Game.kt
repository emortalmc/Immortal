package emortal.immortal.game

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.EventNode
import net.minestom.server.extensions.Extension
import net.minestom.server.instance.Instance
import net.minestom.server.scoreboard.Sidebar
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.Task
import world.cepi.kstom.adventure.sendMiniMessage
import world.cepi.kstom.util.MinestomRunnable
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

abstract class Game(
    val instances: Set<Instance>,
    val gameName: String,
    sidebarTitle: Component,
    extension: Extension,
    val instanceCount: Int = 1,
    val maxPlayerCount: Int = 8,
    val playersToStart: Int = 2,
    val joinableMidGame: Boolean = false
) {
    val id = GameManager.nextGameID

    val eventNode = extension.eventNode.addChild(EventNode.all("$gameName$id"))

    val players: ConcurrentHashMap.KeySetView<Player, Boolean> = ConcurrentHashMap.newKeySet()
    val playerAudience = Audience.audience(players)
    var gameState = GameState.WAITING_FOR_PLAYERS
    val firstInstance get() = instances.first()

    var startingTask: Task? = null

    val scoreboard = Sidebar(sidebarTitle)

    init {
        if (GameManager.gameMap.containsKey(gameName)) {
            GameManager.gameMap[gameName]!!.add(this)
        } else {
            GameManager.gameMap[gameName] = mutableSetOf(this)
        }

        GameManager.nextGameID++

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
    }

    fun addPlayer(player: Player) {
        println("${player.name} joining game $gameName (ID: $id)")
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

        if (players.size == maxPlayerCount) {
            start()
            return
        }

        if (players.size >= playersToStart) {
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
        println("${player.name} leaving game $gameName (ID: $id)")
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
        GameManager.gameMap[gameName]!!.remove(this)

        players.forEach {
            scoreboard.removeViewer(it)
            GameManager.nextGame(gameName)!!.addPlayer(it)
        }
        players.clear()

        eventNode.removeChild(eventNode)

        postDestroy()
    }
    abstract fun postDestroy()

    open fun canBeJoined(): Boolean {
        if (gameState == GameState.PLAYING) {
            return joinableMidGame
        }
        return gameState.joinable
    }

}