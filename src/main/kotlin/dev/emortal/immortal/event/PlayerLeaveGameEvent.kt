package dev.emortal.immortal.event

import dev.emortal.immortal.game.Game
import net.minestom.server.entity.Player

class PlayerLeaveGameEvent(private val game: Game, private val player: Player) : GameEvent {
    override fun getGame(): Game = this.game
    override fun getPlayer(): Player = this.player
}