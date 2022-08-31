package dev.emortal.immortal.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration

fun ComponentLike.armify(length: Int = 79): Component {
    return Component.text()
        .append(Component.text(" ".repeat(length) + "\n", NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH))
        .append(this)
        .append(Component.text("\n" + " ".repeat(length), NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH))
        .build()
}

fun safeStyle(color: TextColor?, decoration: TextDecoration? = null): Style {
    val builder = Style.style()
    builder.color(color)
    if (decoration != null) builder.decorate(decoration)
    return builder.build()
}
fun safeStyle(color: TextColor?, vararg decorations: TextDecoration): Style {
    val builder = Style.style()
    builder.color(color)
    if (decorations.isNotEmpty()) builder.decorate(*decorations)
    return builder.build()
}

private const val CENTER_PX = 154
fun centerText(message: String, bold: Boolean = false): String {
    val halvedMessageSize = message.sumOf { DefaultFontInfo.getLength(it) + if (bold) 2 else 1 } / 2
    val toCompensate = CENTER_PX - halvedMessageSize
    val spaceLength = DefaultFontInfo.SPACE.length + if (bold) 2 else 1

    val spaceCount = toCompensate / spaceLength
    // Couldn't be centered - too long
    if (1 > spaceCount) return message

    return "${" ".repeat(spaceCount)}${message}"
}
fun centerSpaces(message: String, bold: Boolean = false): String {
    val halvedMessageSize = message.sumOf { DefaultFontInfo.getLength(it) + if (bold) 2 else 1 } / 2
    val toCompensate = CENTER_PX - halvedMessageSize
    val spaceLength = DefaultFontInfo.SPACE.length + if (bold) 2 else 1

    val spaceCount = toCompensate / spaceLength
    // Couldn't be centered - too long
    if (1 > spaceCount) return ""

    return " ".repeat(toCompensate / spaceLength)
}
