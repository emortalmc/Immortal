package emortal.immortal.game

import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import java.util.concurrent.ConcurrentHashMap

abstract class Game(
    val instanceCount: Int = 1,
    val maxPlayerCount: Int = 8,
    val instances: Set<Instance>,
    val joinableMidGame: Boolean = false
) {
    val id = GameManager.nextGameID

    val players = ConcurrentHashMap.newKeySet<Player>()
    val gameState = GameState.WAITING_FOR_PLAYERS
    val firstInstance get() = instances.first()

    abstract fun playerJoin(player: Player)
    abstract fun playerLeave(player: Player)

    abstract fun start()
    abstract fun destroy()

    open fun canBeJoined(): Boolean {
        if (gameState == GameState.PLAYING) {
            return joinableMidGame
        }
        return gameState.joinable
    }

}