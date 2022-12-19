package dev.emortal.immortal.util

fun Long.parsed(): String {
    if (this == 0L) return "0s"

    val stringBuilder = StringBuilder()
    val days = this / 86400
    val hours = (this % 86400) / 3600
    val minutes = (this % 3600) / 60
    val seconds = this % 60

    if (days > 0) stringBuilder.append(days).append("d ")
    if (hours > 0) stringBuilder.append(hours).append("h ")
    if (minutes > 0) stringBuilder.append(minutes).append("m ")
    if (seconds > 0) stringBuilder.append(seconds).append("s ")

    return stringBuilder.toString().trim()
}
fun Int.parsed(): String {
    if (this == 0) return "0s"

    val stringBuilder = StringBuilder()
    val days = this / 86400
    val hours = (this % 86400) / 3600
    val minutes = (this % 3600) / 60
    val seconds = this % 60

    if (days > 0) stringBuilder.append(days).append("d ")
    if (hours > 0) stringBuilder.append(hours).append("h ")
    if (minutes > 0) stringBuilder.append(minutes).append("m ")
    if (seconds > 0) stringBuilder.append(seconds).append("s ")

    return stringBuilder.toString().trim()
}

val smallFont = arrayOf(
    'ᴀ', 'ʙ', 'ᴄ', 'ᴅ', 'ᴇ', 'ꜰ', 'ɢ', 'ʜ', 'ɪ', 'ᴊ', 'ᴋ', 'ʟ', 'ᴍ', 'ɴ', 'ᴏ', 'ᴘ', 'ǫ', 'ʀ', 'ѕ', 'ᴛ', 'ᴜ', 'ᴠ', 'ᴡ', 'х', 'ʏ', 'ᴢ',
    '₀', '₁', '₂', '₃', '₄', '₅', '₆', '₇', '₈', '₉'
)
fun String.smallText(): String {
    val stringBuilder = StringBuilder()

    this.lowercase().forEach { char ->
        when (char.code) {
            // a - z
            in 97..122 -> stringBuilder.append(smallFont[char.code - 97])

            // 0 - 9
            in 48..57 -> stringBuilder.append(smallFont[char.code + 26 - 48])

            else -> stringBuilder.append(char)
        }
    }

    return stringBuilder.toString()
}