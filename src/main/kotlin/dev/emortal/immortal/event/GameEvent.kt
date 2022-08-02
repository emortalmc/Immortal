package dev.emortal.immortal.event

import dev.emortal.immortal.game.Game
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.instance.Instance

interface GameEvent : InstanceEvent {

    fun getGame(): Game

    // Game instance is weak reference and nullable, but we cannot change InstanceEvent
    override fun getInstance(): Instance = getGame().instance.get()!!

}