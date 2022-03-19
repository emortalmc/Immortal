package dev.emortal.immortal.config

import dev.emortal.immortal.game.WhenToRegisterEvents
import net.kyori.adventure.text.Component
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode

@kotlinx.serialization.Serializable
data class GameConfig(
    val gameName: String,
    val serverName: String,
    val gameTitle: String,
    val showsInSlashPlay: Boolean = true,
    val spectatable: Boolean = true,
    val whenToRegisterEvents: WhenToRegisterEvents = WhenToRegisterEvents.GAME_START,
    val gameOptions: GameOptions
)