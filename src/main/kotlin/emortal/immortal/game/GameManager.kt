package emortal.immortal.game

import net.minestom.server.entity.Player
import java.util.concurrent.ConcurrentHashMap

object GameManager {
    private val playerGameMap = ConcurrentHashMap<Player, Game>()

    private val gameMap = ConcurrentHashMap<String, Set<Game>>()

    @Volatile
    private var nextGameID: Int = 0

    fun createGame(game: Game) {

    }

    fun nextGame(name: String): Game {
        gameMap[name]!!.firstOrNull { it }
            ?: createGame()
    }
}