package dev.emortal.immortal.config

import dev.emortal.immortal.game.Game
import dev.emortal.immortal.game.WhenToRegisterEvents
import net.kyori.adventure.text.Component
import kotlin.reflect.KClass

data class GameTypeInfo(
    val clazz: KClass<out Game>,
    val name: String,
    val title: Component,
    val showsInSlashPlay: Boolean = true,
    val spectatable: Boolean = true,
    val whenToRegisterEvents: WhenToRegisterEvents = WhenToRegisterEvents.GAME_START,
    val options: GameOptions
)