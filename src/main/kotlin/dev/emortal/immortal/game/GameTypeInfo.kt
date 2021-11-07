package dev.emortal.immortal.game

import net.kyori.adventure.text.Component
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.instance.Instance

data class GameTypeInfo(
    val eventNode: EventNode<Event>,
    val gameName: String,
    val sidebarTitle: Component?,
    val showsInSlashPlay: Boolean = true,
    val whenToRegisterEvents: WhenToRegisterEvents = WhenToRegisterEvents.GAME_START,
    val defaultGameOptions: GameOptions
)