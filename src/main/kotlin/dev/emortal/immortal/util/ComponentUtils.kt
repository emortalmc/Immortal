package dev.emortal.immortal.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

fun ComponentLike.armify(length: Int = 65): Component {
    return Component.text()
        .append(Component.text(" ".repeat(length) + "\n", NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH))
        .append(this)
        .append(Component.text("\n" + " ".repeat(length), NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH))
        .build()
}

private const val CENTER_PX = 154
fun centerText(message: String): String {
    val halvedMessageSize = message.sumOf { DefaultFontInfo.getLength(it) + 1 } / 2
    val toCompensate = CENTER_PX - halvedMessageSize
    val spaceLength = DefaultFontInfo.SPACE.length + 1
    return "${" ".repeat(toCompensate / spaceLength)}${message}"
}