package emortal.immortal.game

import net.kyori.adventure.text.Component
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.instance.Instance

class GameOptions(
    val eventNode: EventNode<Event>,
    val gameName: String,
    val playersToStart: Int,
    val maxPlayers: Int,
    val sidebarTitle: Component,
    val joinableMidGame: Boolean = false,
    val instanceCount: Int = 1,
    val instances: Set<Instance>
)