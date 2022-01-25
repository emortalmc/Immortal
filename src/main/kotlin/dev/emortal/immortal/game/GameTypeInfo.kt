package dev.emortal.immortal.game

import net.kyori.adventure.text.Component
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode

data class GameTypeInfo(
    val eventNode: EventNode<Event>,
    val gameName: String,
    val gameTitle: Component,
    val showsInSlashPlay: Boolean = true,
    val whenToRegisterEvents: WhenToRegisterEvents = WhenToRegisterEvents.GAME_START,
    val gamePresets: MutableMap<String, GameOptions>
)