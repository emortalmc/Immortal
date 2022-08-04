package dev.emortal.immortal.event

import dev.emortal.immortal.game.Game
import net.minestom.server.entity.Player
import net.minestom.server.event.trait.PlayerEvent

class PlayerJoinGameEvent(private val game: Game, private val player: Player) : GameEvent, PlayerEvent {
    override fun getGame(): Game = this.game
    override fun getPlayer(): Player = this.player
}