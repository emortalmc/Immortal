package dev.emortal.immortal.pool

import dev.emortal.immortal.game.Game
import dev.emortal.immortal.game.GameManager
import net.minestom.server.entity.Player
import java.util.concurrent.CompletableFuture

class GameQueue(val pool: Pool<Game>, private val gameName: String) {

    private var current: CompletableFuture<Game> = pool.acquire()

    fun next(player: Player): CompletableFuture<Game> {
        if (!current.join().canBeJoined(player)) {
            current = pool.acquire()
        }

        // Makes sure games do not get abandoned and never start
        val firstNotFull = GameManager.gameMap[gameName]?.values
            ?.filter { it.players.isNotEmpty() }
            ?.sortedBy { it.players.size }
            ?.firstOrNull { it.canBeJoined(player) }
        if (firstNotFull?.createFuture != null) return firstNotFull.createFuture!!.thenApply { firstNotFull }

        return current
    }
}