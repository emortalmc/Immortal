package dev.emortal.immortal.event

import dev.emortal.immortal.game.Game
import net.minestom.server.event.trait.EntityInstanceEvent
import net.minestom.server.event.trait.PlayerEvent

interface GameEvent : PlayerEvent, EntityInstanceEvent {

    fun getGame(): Game

}