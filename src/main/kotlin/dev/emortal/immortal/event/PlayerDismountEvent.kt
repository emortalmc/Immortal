package dev.emortal.immortal.event

import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.event.trait.PlayerEvent

class PlayerDismountEvent(val dismounter: Player, val dismounted: Entity) : PlayerEvent {

    override fun getPlayer(): Player = dismounter

}