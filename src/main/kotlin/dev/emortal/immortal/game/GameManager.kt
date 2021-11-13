package dev.emortal.immortal.game

import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.tag.Tag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

object GameManager {
    val LOGGER: Logger = LoggerFactory.getLogger("GameManager")

    val gameNameTag = Tag.String("gameName")
    val gameIdTag = Tag.Integer("gameId")

    val playerGameMap = ConcurrentHashMap<Player, Game>()
    //KClass<out Game>
    val gameNameToClassMap = ConcurrentHashMap<String, KClass<out Game>>()
    val registeredGameMap = ConcurrentHashMap<KClass<out Game>, GameTypeInfo>()
    val gameMap = ConcurrentHashMap<String, MutableSet<Game>>()

    @Volatile
    var nextGameID = 0

    val Player.game get() = playerGameMap[this]

    fun Player.joinGameOrNew(gameTypeName: String, options: GameOptions = registeredGameMap[gameNameToClassMap[gameTypeName]]!!.defaultGameOptions): Game {
        this.game?.removePlayer(this)

        val game = gameMap[gameTypeName]?.firstOrNull {
            // TODO: has to be changed for parties
            it.canBeJoined()
        }
            ?: createGame(gameTypeName, options)

        game.addPlayer(this)

        return game
    }

    //inline fun <reified T : Game> Player.joinGameOrNew(options: GameOptions = registeredGameMap[gameTypeName]!!.defaultGameOptions): Game = this.joinGameOrNew(T::class, options)

    fun createGame(gameTypeName: String, options: GameOptions): Game {
        nextGameID++

        val gameClass = gameNameToClassMap[gameTypeName]
        val game = gameClass?.primaryConstructor?.call(options) ?: throw IllegalArgumentException("Primary constructor not found.")

        gameMap.getOrDefault(gameTypeName, mutableSetOf()).add(game)

        return game
    }

    inline fun <reified T: Game> registerGame(
        eventNode: EventNode<Event>,
        gameName: String,
        sidebarTitle: Component,
        showsInSlashPlay: Boolean = true,
        whenToRegisterEvents: WhenToRegisterEvents = WhenToRegisterEvents.GAME_START,
        defaultGameOptions: GameOptions
    ) {
        gameNameToClassMap[gameName] = T::class

        registeredGameMap[T::class] = GameTypeInfo(
            eventNode,
            gameName,
            sidebarTitle,
            showsInSlashPlay,
            whenToRegisterEvents,
            defaultGameOptions
        )

        LOGGER.info("Registered game type '${gameName}'")
    }
}