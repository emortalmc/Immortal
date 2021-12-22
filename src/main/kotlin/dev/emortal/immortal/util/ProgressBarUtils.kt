package dev.emortal.immortal.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import java.time.format.TextStyle
import kotlin.math.roundToInt

fun progressBar(percentage: Float, characterCount: Int = 10, character: String = " ", completeColor: NamedTextColor, incompleteColor: NamedTextColor, decoration: TextDecoration? = null): Component {
    val percentage = percentage.coerceIn(0f, 1f)

    val completeCharacters = percentage.roundToInt() * characterCount
    val incompleteCharacters = (1 - percentage).roundToInt() * characterCount

    if (decoration == null) {
        return Component.text(character.repeat(completeCharacters), completeColor)
            .append(Component.text(character.repeat(incompleteCharacters), incompleteColor))
    } else {
        return Component.text(character.repeat(completeCharacters), completeColor, decoration)
            .append(Component.text(character.repeat(incompleteCharacters), incompleteColor, decoration))
    }


}