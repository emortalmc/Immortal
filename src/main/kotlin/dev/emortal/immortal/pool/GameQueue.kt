package dev.emortal.immortal.pool

import dev.emortal.immortal.game.Game
import net.minestom.server.entity.Player

class GameQueue(private val pool: Pool<Game>) {

    private var current: Game = pool.acquire().join()

    fun next(player: Player): Game {
        if (!current.canBeJoined(player) || !current.instance.isRegistered) {
            current = pool.acquire().join()
        }
        return current
    }
}