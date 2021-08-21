package emortal.immortal.game

import emortal.immortal.game.GameManager.game
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

object GameManager {
    val playerGameMap = ConcurrentHashMap<Player, Game>()
    val registeredGameMap = ConcurrentHashMap<KClass<out Game>, GameTypeInfo>()
    val gameMap = ConcurrentHashMap<KClass<out Game>, MutableSet<Game>>()

    @Volatile
    var nextGameID = 0

    val Player.game get() = playerGameMap[this]

    fun <T : Game> Player.joinGameOrNew(clazz: KClass<T>, options: GameOptions): Game {
        if (this.game != null) {
            this.game!!.removePlayer(this)
        }

        val game = gameMap[clazz]?.firstOrNull {
            it.canBeJoined()
        }
            ?: createGame(clazz, options)

        game.addPlayer(this)

        return game
    }
    inline fun <reified T : Game> Player.joinGameOrNew(options: GameOptions): Game = this.joinGameOrNew(T::class, options)

    fun <T : Game> createGame(gameType: KClass<T>, options: GameOptions): Game {
        nextGameID++

        val game = gameType.primaryConstructor?.call(options) ?: throw IllegalArgumentException("Primary constructor not found.")

        gameMap.putIfAbsent(gameType, mutableSetOf())
        gameMap[gameType]!!.add(game)

        return game
    }

    inline fun <reified T: Game> registerGame(gameOptions: GameTypeInfo) {
        println("Registered game type '${gameOptions.gameName}'")
        registeredGameMap[T::class] = gameOptions
    }
}