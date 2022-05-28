package dev.emortal.immortal.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.util.RGBLike
import kotlin.math.ceil
import kotlin.math.floor

fun progressBar(percentage: Float, characterCount: Int = 10, character: String = " ", completeColor: RGBLike, incompleteColor: RGBLike, decoration: TextDecoration? = null): Component {
    val percentage = percentage.coerceIn(0f, 1f)

    val completeCharacters = ceil((percentage * characterCount)).toInt()
    val incompleteCharacters = floor((1 - percentage) * characterCount).toInt()

    return if (decoration == null) {
        Component.text(character.repeat(completeCharacters), TextColor.color(completeColor))
            .append(Component.text(character.repeat(incompleteCharacters), TextColor.color(incompleteColor)))
    } else {
        Component.text(character.repeat(completeCharacters), TextColor.color(completeColor), decoration)
            .append(Component.text(character.repeat(incompleteCharacters), TextColor.color(incompleteColor), decoration))
    }


}