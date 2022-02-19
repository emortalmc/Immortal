package dev.emortal.immortal.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

fun Component.plain(): String = PlainTextComponentSerializer.plainText().serialize(this)