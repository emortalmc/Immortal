package dev.emortal.immortal.config

import dev.emortal.immortal.game.Game
import net.kyori.adventure.text.Component
import kotlin.reflect.KClass

data class GameTypeInfo(
    val clazz: KClass<out Game>,
    val name: String,
    val title: Component,
    val showsInSlashPlay: Boolean = true
)