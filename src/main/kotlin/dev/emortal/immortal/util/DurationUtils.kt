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