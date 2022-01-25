package dev.emortal.immortal.game

import dev.emortal.acquaintance.RelationshipManager.party
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

object GameManager {
    val logger: Logger = LoggerFactory.getLogger("GameManager")

    val gameNameTag = Tag.String("gameName")
    val gameIdTag = Tag.Integer("gameId")

    val playerGameMap = ConcurrentHashMap<Player, Game>()
    val spectatorToFriendMap = ConcurrentHashMap<Player, Player>()

    val gameNameToClassMap = ConcurrentHashMap<String, KClass<out Game>>()
    val registeredGameMap = ConcurrentHashMap<KClass<out Game>, GameTypeInfo>()
    val gameMap = ConcurrentHashMap<String, MutableSet<Game>>()

    val joiningGameTag = Tag.Byte("joiningGame")

    @Volatile
    var nextGameID = 0

    var Player.game get() = playerGameMap[this]
        set(value) {
            if (value == null) return
            this.joinGame(value)
        }

    fun Player.joinGame(game: Game): CompletableFuture<Boolean> {
        val future: CompletableFuture<Boolean> = CompletableFuture()
        val lastGame = this.game

        if (hasTag(joiningGameTag)) {
            sendMessage(Component.text("Already joining a game", NamedTextColor.RED))
            future.complete(false)
            return future
        }
        setTag(joiningGameTag, 1)

        sendActionBar(
            Component.text()
                .append(Component.text("Joining ", NamedTextColor.GREEN))
                .append(game.gameTypeInfo.gameTitle)
        )

        playerGameMap[this] = game

        game.addPlayer(this).thenAccept { wasSuccessful ->
            if (wasSuccessful) {
                lastGame?.removePlayer(this)
            } else {
                sendMessage(Component.text("Something went wrong while joining ${game.gameTypeInfo.gameName}", NamedTextColor.RED))
                playSound(Sound.sound(SoundEvent.ENTITY_VILLAGER_NO, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self())
            }
            removeTag(joiningGameTag)
            future.complete(wasSuccessful)
        }

        return future
    }

    fun Player.joinGameOrNew(
        gameTypeName: String,
        options: GameOptions = registeredGameMap[gameNameToClassMap[gameTypeName]]!!.gamePresets["default"]!!
    ): CompletableFuture<Boolean> = this.joinGame(findOrCreateGame(this, gameTypeName, options))

    fun findOrCreateGame(
        player: Player,
        gameTypeName: String,
        options: GameOptions = registeredGameMap[gameNameToClassMap[gameTypeName]]!!.gamePresets["default"]!!
    ): Game {
        val game = gameMap[gameTypeName]?.firstOrNull {
            // TODO: Private games with parties
            it.canBeJoined(player) && it.gameOptions == options
        }
            ?: createGame(gameTypeName, options)

        return game
    }

    fun createGame(gameTypeName: String, options: GameOptions): Game {
        nextGameID++

        val gameClass = gameNameToClassMap[gameTypeName]
        val game = gameClass?.primaryConstructor?.call(options)
            ?: throw IllegalArgumentException("Primary constructor not found.")

        gameMap.putIfAbsent(gameTypeName, mutableSetOf())
        gameMap[gameTypeName]!!.add(game)

        return game
    }

    inline fun <reified T : Game> registerGame(
        eventNode: EventNode<Event>,
        gameName: String,
        sidebarTitle: Component,
        showsInSlashPlay: Boolean = true,
        spectatable: Boolean = true,
        whenToRegisterEvents: WhenToRegisterEvents = WhenToRegisterEvents.GAME_START,
        defaultGameOptions: GameOptions
    ) {
        gameNameToClassMap[gameName] = T::class

        registeredGameMap[T::class] = GameTypeInfo(
            eventNode,
            gameName,
            sidebarTitle,
            showsInSlashPlay,
            spectatable,
            whenToRegisterEvents,
            mutableMapOf("default" to defaultGameOptions)
        )

        logger.info("Registered game type '${gameName}'")
    }

    inline fun <reified  T : Game> addPreset(presetName: String, presetOptions: GameOptions) {
        registeredGameMap[T::class]?.gamePresets?.set(presetName, presetOptions)
    }

    inline fun <reified T : Game> unregisterGame() {
        val gameName = registeredGameMap[T::class]!!.gameName

        gameMap[gameName]?.forEach {
            it.destroy()
        }
        gameMap.remove(gameName)
        gameNameToClassMap.remove(gameName)
        registeredGameMap.remove(T::class)

        logger.info("Unregistered game type '${gameName}'")
    }
}