package dev.emortal.immortal.game

import net.minestom.server.entity.Entity
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player

abstract class PvpGame(gameOptions: GameOptions) : Game(gameOptions) {

    // TODO: Make messages configurable
    fun kill(player: Player, killer: Entity? = null) {
        player.clearEffects()
        player.heal()
        player.isInvisible = true
        player.gameMode = GameMode.SPECTATOR
        // TODO: global message when messages are configurable

        playerDied(player, killer)
    }

    abstract fun playerDied(player: Player, killer: Entity?)

    abstract fun respawn(player: Player)

}