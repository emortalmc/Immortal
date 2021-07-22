package emortal.immortal.game

import net.minestom.server.entity.Player
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

object GameManager {
    val playerGameMap = ConcurrentHashMap<Player, Game>()
    val registeredGameMap = ConcurrentHashMap<KClass<out Game>, GameOptions>()
    val gameMap = ConcurrentHashMap<KClass<out Game>, MutableSet<Game>>()

    @Volatile
    var nextGameID = 0

    fun <T : Game> Player.joinGameOrNew(clazz: KClass<T>) {
        val game = gameMap[clazz]?.firstOrNull {
            it.canBeJoined()
        }
            ?: createGame(clazz)

        game.addPlayer(this)
    }
    inline fun <reified T : Game> Player.joinGameOrNew() = this.joinGameOrNew(T::class)

    fun <T : Game> createGame(gameType: KClass<T>): Game {
        nextGameID++
        return gameType.primaryConstructor?.call(registeredGameMap[gameType]) ?: throw IllegalArgumentException("Primary constructor not found.")
    }

    inline fun <reified T: Game> registerGame(gameOptions: GameOptions) {
        registeredGameMap[T::class] = gameOptions
    }
}