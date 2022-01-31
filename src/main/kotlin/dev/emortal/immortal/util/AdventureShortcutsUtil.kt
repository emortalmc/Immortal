package dev.emortal.immortal.util

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.title.Title
import net.minestom.server.coordinate.Point
import java.time.Duration

fun Component.toPlainString(): String = PlainTextComponentSerializer.plainText().serialize(this)

fun Audience.playSound(soundEvent: Sound.Type, source: Sound.Source, volume: Float, pitch: Float, emitter: Sound.Emitter = Sound.Emitter.self()) {
    playSound(Sound.sound(soundEvent, source, volume, pitch), emitter)
}

fun Audience.playSound(soundEvent: Sound.Type, source: Sound.Source, volume: Float, pitch: Float, position: Point) {
    playSound(Sound.sound(soundEvent, source, volume, pitch), position.x(), position.y(), position.z())
}

fun Audience.showTitle(title: Component, subtitle: Component, fadeIn: Duration, stay: Duration, fadeOut: Duration) {
    showTitle(Title.title(title, subtitle, Title.Times.of(fadeIn, stay, fadeOut)))
}

fun TextComponent.Builder.append(value: Any, textColor: TextColor = NamedTextColor.WHITE, decoration: TextDecoration? = null) {
    append(Component.text(value.toString(), if (decoration == null) Style.style(textColor) else Style.style(textColor, decoration)))
}