package dev.emortal.immortal.event

import dev.emortal.immortal.game.Game
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.instance.Instance

interface GameEvent : InstanceEvent {

    fun getGame(): Game

    override fun getInstance(): Instance = getGame().instance

}