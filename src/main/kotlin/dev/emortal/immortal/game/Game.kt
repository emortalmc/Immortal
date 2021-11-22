package dev.emortal.immortal.game

import dev.emortal.immortal.game.GameManager.gameIdTag
import dev.emortal.immortal.game.GameManager.gameNameTag
import dev.emortal.immortal.game.GameManager.joinGameOrNew
import dev.emortal.immortal.inventory.SpectatingGUI
import dev.emortal.immortal.util.reset
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import net.minestom.server.adventure.audience.PacketGroupingAudience
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.EventFilter
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.instance.Instance
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.scoreboard.Sidebar
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.Task
import org.slf4j.LoggerFactory
import world.cepi.kstom.event.listenOnly
import world.cepi.kstom.util.MinestomRunnable
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

abstract class Game(val gameOptions: GameOptions) : PacketGroupingAudience {

    val players: MutableSet<Player> = ConcurrentHashMap.newKeySet()
    val spectators: MutableSet<Player> = ConcurrentHashMap.newKeySet()
    val teams = mutableSetOf<Team>()

    var gameState = GameState.WAITING_FOR_PLAYERS
    val gameTypeInfo = GameManager.registeredGameMap[this::class] ?: throw Error("Game type not registered")
    val id = GameManager.nextGameID

    val LOGGER = LoggerFactory.getLogger("Game-${gameTypeInfo.gameName}")

    val instance: Instance = instanceCreate().also {
        it.setTag(gameNameTag, gameTypeInfo.gameName)
        it.setTag(gameIdTag, id)
    }

    val eventNode = EventNode.tag(
        "${gameTypeInfo.gameName}-$id", EventFilter.INSTANCE,
        gameIdTag
    ) { it == id }

    var startingTask: Task? = null
    var scoreboard: Sidebar? = null

    val spectatorGUI = SpectatingGUI(this)

    init {
        gameTypeInfo.eventNode.addChild(eventNode)

        eventNode.listenOnly<PlayerUseItemEvent> {
            if (!spectators.contains(player)) return@listenOnly
            if (player.itemInMainHand.material != Material.COMPASS) return@listenOnly

            player.openInventory(spectatorGUI.inventory)
        }

        if (gameTypeInfo.whenToRegisterEvents == WhenToRegisterEvents.IMMEDIATELY) registerEvents()

        if (gameOptions.showScoreboard) {
            scoreboard = gameTypeInfo.sidebarTitle?.let { Sidebar(it) }

            scoreboard?.createLine(Sidebar.ScoreboardLine("HeaderSpacer", Component.empty(), 30))

            scoreboard?.createLine(
                Sidebar.ScoreboardLine(
                    "InfoLine",
                    Component.text()
                        .append(Component.text("Waiting for players...", NamedTextColor.GRAY))
                        .build(),
                    0
                )
            )
            scoreboard?.createLine(Sidebar.ScoreboardLine("FooterSpacer", Component.empty(), -1))
            scoreboard?.createLine(
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

        LOGGER.info("A game of '${gameTypeInfo.gameName}' was created")
    }

    fun addPlayer(player: Player, joinMessage: Boolean = gameOptions.showsJoinLeaveMessages) {
        if (players.contains(player)) return

        LOGGER.info("${player.username} joining game '${gameTypeInfo.gameName}'")

        spectatorGUI.refresh()

        players.add(player)
        scoreboard?.addViewer(player)
        GameManager.playerGameMap[player] = this

        player.reset()

        player.scheduleNextTick {
            if (player.instance!! != instance) player.setInstance(instance)
        }

        if (joinMessage) sendMessage(
            Component.text()
                .append(Component.text("JOIN", NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text(player.username, NamedTextColor.GREEN))
                .append(Component.text(" joined the game ", NamedTextColor.GRAY))
                .append(Component.text("(${players.size}/${gameOptions.maxPlayers})", NamedTextColor.DARK_GRAY))
        )

        playerJoin(player)

        if (players.size >= gameOptions.minPlayers) {
            if (startingTask != null) return

            startCountdown()
        }
    }

    fun removePlayer(player: Player, leaveMessage: Boolean = gameOptions.showsJoinLeaveMessages) {
        if (!players.contains(player)) return

        LOGGER.info("${player.username} leaving game '${gameTypeInfo.gameName}'")

        spectatorGUI.refresh()

        teams.forEach {
            it.remove(player)
        }

        player.reset()

        players.remove(player)
        GameManager.playerGameMap.remove(player)
        scoreboard?.removeViewer(player)

        if (leaveMessage) sendMessage(
            Component.text()
                .append(Component.text("QUIT", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text(player.username, NamedTextColor.RED))
                .append(Component.text(" left the game", NamedTextColor.GRAY))
                .append(Component.text("(${players.size}/${gameOptions.maxPlayers})", NamedTextColor.DARK_GRAY))
        )

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

    fun addSpectator(player: Player, friend: Player) {
        if (spectators.contains(player)) return

        LOGGER.info("${player.username} spectating game '${gameTypeInfo.gameName}'")

        player.reset()
        player.isAutoViewable = false
        player.isInvisible = true
        player.isAllowFlying = true
        player.isFlying = true
        player.gameMode = GameMode.SPECTATOR

        player.inventory.setItemStack(4, ItemStack.of(Material.COMPASS))

        friend.sendMessage(
            Component.text()
                .append(Component.text("FRIEND", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Your friend ", NamedTextColor.GRAY))
                .append(Component.text(player.username, NamedTextColor.GREEN))
                .append(Component.text(" is now spectating your game", NamedTextColor.GRAY))
        )

        GameManager.spectatorToFriendMap[player] = friend

        spectatorJoin(player, friend)
    }

    fun removeSpectator(player: Player) {
        if (!spectators.contains(player)) return

        val friend = GameManager.spectatorToFriendMap[player]

        LOGGER.info("${player.username} stopped spectating game '${gameTypeInfo.gameName}'")

        spectators.add(player)

        friend?.sendMessage(
            Component.text()
                .append(Component.text("FRIEND", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Your friend ", NamedTextColor.GRAY))
                .append(Component.text(player.username, NamedTextColor.RED))
                .append(Component.text(" is no longer spectating your game", NamedTextColor.GRAY))
        )

        player.reset()

        spectatorLeave(player, friend!!)
    }

    open fun spectatorJoin(player: Player, friend: Player) {}
    open fun spectatorLeave(player: Player, friend: Player) {}

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
        scoreboard?.updateLineContent(
            "InfoLine",
            Component.text(
                "Waiting for players... (${gameOptions.minPlayers - players.size} more)",
                NamedTextColor.GRAY
            )
        )
        startingTask?.cancel()
        startingTask = null
        showTitle(
            Title.title(
                Component.text("Start cancelled!", NamedTextColor.RED, TextDecoration.BOLD),
                Component.text("Not enough players"),
                Title.Times.of(Duration.ZERO, Duration.ofSeconds(2), Duration.ofSeconds(1))
            )
        )
        playSound(Sound.sound(SoundEvent.ENTITY_VILLAGER_NO, Sound.Source.AMBIENT, 1f, 1f))
    }

    abstract fun registerEvents()

    fun start() {
        startingTask?.cancel()
        startingTask = null
        gameState = GameState.PLAYING
        scoreboard?.updateLineContent("InfoLine", Component.empty())

        if (gameTypeInfo.whenToRegisterEvents == WhenToRegisterEvents.GAME_START) registerEvents()
        gameStarted()
    }

    fun destroy() {
        LOGGER.info("A game of '${gameTypeInfo.gameName}' is being destroyed")

        gameTypeInfo.eventNode.removeChild(eventNode)

        GameManager.gameMap[gameTypeInfo.gameName]?.remove(this)

        teams.forEach {
            it.destroy()
        }

        players.forEach {
            scoreboard?.removeViewer(it)
            it.joinGameOrNew("lobby")
            //it.joinGameOrNew(gameTypeInfo.gameName, gameOptions)
        }

        spectators.forEach {
            it.joinGameOrNew("lobby")
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

    override fun getPlayers(): MutableCollection<Player> = (players + spectators).toMutableSet()

}
