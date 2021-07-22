package emortal.immortal.game

import net.minestom.server.entity.Player
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

    fun <T : Game> Player.joinGameOrNew(clazz: KClass<T>, options: GameOptions) {
        val game = gameMap[clazz]?.firstOrNull {
            it.canBeJoined()
        }
            ?: createGame(clazz, options)

        game.addPlayer(this)
    }
    inline fun <reified T : Game> Player.joinGameOrNew(options: GameOptions) = this.joinGameOrNew(T::class, options)

    fun <T : Game> createGame(gameType: KClass<T>, options: GameOptions): Game {
        nextGameID++
        return gameType.primaryConstructor?.call(options) ?: throw IllegalArgumentException("Primary constructor not found.")
    }

    inline fun <reified T: Game> registerGame(gameOptions: GameTypeInfo) {
        registeredGameMap[T::class] = gameOptions
    }
}