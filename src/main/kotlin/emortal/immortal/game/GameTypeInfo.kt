package emortal.immortal.game

import net.kyori.adventure.text.Component
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.instance.Instance

class GameTypeInfo(
    val eventNode: EventNode<Event>,
    val gameName: String,
    val sidebarTitle: Component?,
    val showsInPlayCommand: Boolean = true,
    val defaultGameOptions: GameOptions
)