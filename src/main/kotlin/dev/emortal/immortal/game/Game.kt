package dev.emortal.immortal.game

import dev.emortal.immortal.game.GameManager.gameIdTag
import dev.emortal.immortal.game.GameManager.gameNameTag
import dev.emortal.immortal.game.GameManager.joinGameOrNew
import dev.emortal.immortal.inventory.SpectatingGUI
import dev.emortal.immortal.util.reset
import dev.emortal.immortal.util.task
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
import world.cepi.kstom.Manager
import world.cepi.kstom.event.listenOnly
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

    val instances = mutableListOf(instance)

    val eventNode = EventNode.tag(
        "${gameTypeInfo.gameName}-$id", EventFilter.INSTANCE,
        gameIdTag
    ) { it == id }

    var startingTask: Task? = null
    var scoreboard: Sidebar? = null

    val spectatorGUI = SpectatingGUI()

    init {
        gameTypeInfo.eventNode.addChild(eventNode)

        eventNode.listenOnly<PlayerUseItemEvent> {
            if (!spectators.contains(player)) return@listenOnly
            if (player.itemInMainHand.material != Material.COMPASS) return@listenOnly
            player.openInventory(spectatorGUI.inventory)
        }

        if (gameTypeInfo.whenToRegisterEvents == WhenToRegisterEvents.IMMEDIATELY) registerEvents()

        if (gameOptions.showScoreboard) {
            scoreboard = gameTypeInfo.gameTitle?.let { Sidebar(it) }

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

    internal fun addPlayer(player: Player, joinMessage: Boolean = gameOptions.showsJoinLeaveMessages) {
        if (players.contains(player)) return

        LOGGER.info("${player.username} joining game '${gameTypeInfo.gameName}'")

        players.add(player)
        scoreboard?.addViewer(player)

        player.reset()

        spectatorGUI.refresh(players)

        if (player.instance!! != instance) player.setInstance(instance)

        if (joinMessage) sendMessage(
            Component.text()
                .append(Component.text("JOIN", NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text(player.username, NamedTextColor.GREEN))
                .append(Component.text(" joined the game ", NamedTextColor.GRAY))
                .append(Component.text("(${players.size}/${gameOptions.maxPlayers})", NamedTextColor.DARK_GRAY))
        )
        playSound(Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.MASTER, 1f, 1.2f))

        player.scheduleNextTick {
            playerJoin(player)
        }

        if (gameState == GameState.WAITING_FOR_PLAYERS && players.size >= gameOptions.minPlayers) {
            if (startingTask != null) return

            startCountdown()
        }
    }

    internal fun removePlayer(player: Player, leaveMessage: Boolean = gameOptions.showsJoinLeaveMessages) {
        if (!players.contains(player)) return

        LOGGER.info("${player.username} leaving game '${gameTypeInfo.gameName}'")

        spectatorGUI.refresh(players)

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
                .append(Component.text(" left the game ", NamedTextColor.GRAY))
                .append(Component.text("(${players.size}/${gameOptions.maxPlayers})", NamedTextColor.DARK_GRAY))
        )
        playSound(Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.MASTER, 1f, 0.5f))

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
        player.respawnPoint = friend.position
        if (player.instance != instance) player.setInstance(instance)
        player.isAutoViewable = false
        player.isInvisible = true
        player.gameMode = GameMode.ADVENTURE
        player.isAllowFlying = true
        player.isFlying = true

        player.inventory.setItemStack(4, ItemStack.of(Material.COMPASS))

        spectators.add(player)

        friend.sendMessage(
            Component.text()
                .append(Component.text("FRIEND", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Your friend ", NamedTextColor.GRAY))
                .append(Component.text(player.username, NamedTextColor.GREEN))
                .append(Component.text(" is now spectating your game", NamedTextColor.GRAY))
        )

        player.playSound(Sound.sound(SoundEvent.ENTITY_BAT_AMBIENT, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self())

        GameManager.spectatorToFriendMap[player] = friend

        spectatorJoin(player, friend)
    }

    fun removeSpectator(player: Player) {
        if (!spectators.contains(player)) return

        val friend = GameManager.spectatorToFriendMap[player]

        LOGGER.info("${player.username} stopped spectating game '${gameTypeInfo.gameName}'")

        spectators.remove(player)
        player.joinGameOrNew("lobby")

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

        startingTask = task(repeat = Duration.ofSeconds(1), iterations = gameOptions.countdownSeconds, finalIteration = { start() }) { _,i ->
            playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_COW_BELL, Sound.Source.AMBIENT, 1f, 0.5f))
            showTitle(
                Title.title(
                    Component.text(gameOptions.countdownSeconds - i, NamedTextColor.GREEN, TextDecoration.BOLD),
                    Component.empty(),
                    Title.Times.of(
                        Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(250)
                    )
                )
            )
        }
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
        playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self())

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

        instances.forEach {
            Manager.instance.unregisterInstance(it)
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
