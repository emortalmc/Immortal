package emortal.immortal.game

import emortal.immortal.game.GameManager.joinGameOrNew
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.title.Title
import net.minestom.server.entity.Entity
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

    private val gameMiniMessage = MiniMessage.get()

    val instance = gameOptions.instanceCallback.invoke()

    var startingTask: Task? = null

    val childEventNode = gameTypeInfo.eventNode.addChild(EventNode.all("${gameTypeInfo.gameName}${GameManager.nextGameID}"))
    val scoreboard = Sidebar(gameTypeInfo.sidebarTitle)

    init {
        scoreboard.createLine(Sidebar.ScoreboardLine("header", Component.empty(), 30))
        scoreboard.createLine(
            Sidebar.ScoreboardLine(
                "InfoLine",
                Component.text()
                    .append(Component.text("Waiting for players...", NamedTextColor.GRAY))
                    .build(),
                0
            )
        )
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
        println("${player.username} joining game ${gameTypeInfo.gameName}")
        players.add(player)
        scoreboard.addViewer(player)
        GameManager.playerGameMap[player] = this

        player.isInvisible = false
        player.gameMode = GameMode.SPECTATOR

        playerAudience.sendMiniMessage(" <green><bold>JOIN</bold></green> <dark_gray>|</dark_gray> ${player.username}")

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

            scoreboard.updateLineContent("InfoLine", Component.text("Starting...", NamedTextColor.GRAY))

            startingTask = object : MinestomRunnable() {
                var secs = 5

                override fun run() {
                    if (secs < 1) {
                        cancel()
                        start()

                        scoreboard.updateLineContent("InfoLine", Component.empty())

                        gameState = GameState.PLAYING
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
        println("${player.username} leaving game ${gameTypeInfo.gameName}")
        players.remove(player)
        GameManager.playerGameMap.remove(player)
        scoreboard.removeViewer(player)

        player.isInvisible = false

        playerAudience.sendMiniMessage(" <red><bold>LEAVE</bold></red> <dark_gray>|</dark_gray> ${player.username}")

        if (players.size < gameOptions.playersToStart) {
            if (startingTask != null && gameState == GameState.STARTING) {
                startingTask?.cancel()
                playerAudience.showTitle(Title.title(Component.text("START CANCELLED", NamedTextColor.RED, TextDecoration.BOLD), Component.empty(), Title.Times.of(Duration.ZERO, Duration.ofSeconds(2), Duration.ofSeconds(1))))
            }
        }

        playerLeave(player)
    }

    abstract fun playerJoin(player: Player)
    abstract fun playerLeave(player: Player)

    abstract fun start()

    fun kill(player: Player, killer: Entity?) {

        val subtitle = Component.empty()

        player.clearEffects()
        player.heal()
        player.isInvisible = true
        player.gameMode = GameMode.SPECTATOR
        player.showTitle(
            Title.title(
                Component.text("YOU DIED", NamedTextColor.RED, TextDecoration.BOLD),
                subtitle,
                Title.Times.of(Duration.ZERO, Duration.ofSeconds(2), Duration.ofSeconds(1))
            )
        )

        playerDied(player, killer)
    }

    abstract fun playerDied(player: Player, killer: Entity?, deathMessage: () -> Component = {
        if (killer == null || killer !is Player) {
            gameMiniMessage.parse(" <red>☠</red> <dark_gray>|</dark_gray> <red>${player.username}</red> died")
        } else {
            gameMiniMessage.parse(" <red>☠</red> <dark_gray>|</dark_gray> <gray><white>${killer.username}</white> killed <red>${player.username}</red>")
        }
    })

    abstract fun respawn(player: Player)

    fun destroy() {
        // Detach child event node to stop game events
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