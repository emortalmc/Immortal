package dev.emortal.immortal.util

import com.google.common.base.Strings
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import kotlin.math.roundToInt

fun progressBar(percentage: Float): Component {
    val percentage = percentage.coerceIn(0f, 1f)

    val characters = 20
    val character = " "
    val completeCharacters = (percentage * characters).roundToInt()
    val incompleteCharacters = ((1 - percentage) * characters).roundToInt()

    return Component.text(Strings.repeat(character, completeCharacters), NamedTextColor.GREEN, TextDecoration.STRIKETHROUGH)
        .append(Component.text(Strings.repeat(character, incompleteCharacters), NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH))
}