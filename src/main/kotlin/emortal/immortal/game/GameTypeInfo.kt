package emortal.immortal.game

import net.kyori.adventure.text.Component
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode

class GameTypeInfo(
    val eventNode: EventNode<Event>,
    val gameName: String,
    val sidebarTitle: Component,
    val defaultGameOptions: GameOptions
)