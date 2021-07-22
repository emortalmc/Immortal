package emortal.immortal.game

import net.minestom.server.entity.Player
import java.util.concurrent.ConcurrentHashMap

object GameManager {
    val playerGameMap = ConcurrentHashMap<Player, Game>()

    val gameMap = ConcurrentHashMap<String, MutableSet<Game>>()

    @Volatile
    var nextGameID: Int = 0

    var Player.game: Game?
        get() = playerGameMap[this]
        set(value) {
            playerGameMap[this]?.removePlayer(this)
            value?.addPlayer(this)
        }

    fun nextGame(name: String): Game? {
        return gameMap[name]!!.firstOrNull { it.canBeJoined() }
    }
}