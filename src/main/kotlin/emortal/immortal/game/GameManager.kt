package emortal.immortal.game

import net.minestom.server.entity.Player
import java.util.concurrent.ConcurrentHashMap

object GameManager {
    private val playerGameMap = ConcurrentHashMap<Player, Game>()

    private val gameMap = ConcurrentHashMap<String, MutableSet<Game>>()

    @Volatile
    var nextGameID: Int = 0

    fun registerGame(name: String, game: Game) {
        if (gameMap.containsKey(name)) {
            gameMap[name]!!.add(game)
        } else {
            gameMap[name] = mutableSetOf(game)
        }

        nextGameID++
    }

    fun Game.addPlayer(player: Player) {
        playerGameMap[player] = this
        this.playerJoin(player)
    }
    fun Game.removePlayer(player: Player) {
        playerGameMap.remove(player)
        this.playerLeave(player)
    }

    fun nextGame(name: String): Game? {
        return gameMap[name]!!.firstOrNull { it.canBeJoined() }
    }
}