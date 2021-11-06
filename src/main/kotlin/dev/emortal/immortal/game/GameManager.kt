package dev.emortal.immortal.game

import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventFilter
import net.minestom.server.event.EventNode
import net.minestom.server.tag.Tag
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

object GameManager {
    val LOGGER = LoggerFactory.getLogger("GameManager")

    val gameNameTag = Tag.String("gameName")
    val gameIdTag = Tag.Integer("gameId")

    val playerGameMap = ConcurrentHashMap<Player, Game>()
    val registeredGameMap = ConcurrentHashMap<KClass<out Game>, GameTypeInfo>()
    val gameMap = ConcurrentHashMap<KClass<out Game>, MutableSet<Game>>()

    @Volatile
    var nextGameID = 0

    val Player.game get() = playerGameMap[this]

    fun <T : Game> Player.joinGameOrNew(clazz: KClass<T>, options: GameOptions = registeredGameMap[clazz]!!.defaultGameOptions): Game {
        if (this.game != null) {
            this.game!!.removePlayer(this)
        }

        val game = gameMap[clazz]?.firstOrNull {
            // TODO: has to be changed for parties
            it.canBeJoined()
        }
            ?: createGame(clazz, options)

        game.addPlayer(this)

        return game
    }

    inline fun <reified T : Game> Player.joinGameOrNew(options: GameOptions = registeredGameMap[T::class]!!.defaultGameOptions): Game = this.joinGameOrNew(T::class, options)

    fun <T : Game> createGame(gameType: KClass<T>, options: GameOptions): Game {
        nextGameID++

        val game = gameType.primaryConstructor?.call(options) ?: throw IllegalArgumentException("Primary constructor not found.")

        gameMap.putIfAbsent(gameType, mutableSetOf())
        gameMap[gameType]!!.add(game)

        return game
    }

    inline fun <reified T: Game> registerGame(
        eventNode: EventNode<Event>,
        gameName: String,
        sidebarTitle: Component,
        showsInSlashPlay: Boolean = true,
        defaultGameOptions: GameOptions
    ) {
        registeredGameMap[T::class] = GameTypeInfo(
            eventNode,
            gameName,
            sidebarTitle,
            showsInSlashPlay,
            defaultGameOptions
        )

        LOGGER.info("Registered game type '${gameName}'")
    }
}