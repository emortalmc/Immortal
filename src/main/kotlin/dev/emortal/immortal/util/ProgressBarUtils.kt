package dev.emortal.immortal.util

import com.google.common.base.Strings
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import kotlin.math.roundToInt

fun progressBar(percentage: Float, characterCount: Int = 10, character: String = " ", completeColor: NamedTextColor, incompleteColor: NamedTextColor): Component {
    val percentage = percentage.coerceIn(0f, 1f)

    val completeCharacters = percentage.roundToInt() * characterCount
    val incompleteCharacters = (1 - percentage).roundToInt() * characterCount

    return Component.text(character.repeat(completeCharacters), completeColor, TextDecoration.STRIKETHROUGH)
        .append(Component.text(character.repeat(incompleteCharacters), incompleteColor, TextDecoration.STRIKETHROUGH))
}