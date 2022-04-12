package dev.emortal.immortal.util

import java.time.Duration

val Long.millis get() = Duration.ofMillis(this)
val Long.seconds get() = Duration.ofSeconds(this)
val Long.minutes get() = Duration.ofMinutes(this)
val Long.hours get() = Duration.ofHours(this)
val Long.days get() = Duration.ofDays(this)

fun Long.parsed(): String {
    if (this == 0L) return "0s"

    val stringBuilder = StringBuilder()
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    val seconds = this % 60

    if (hours > 0) stringBuilder.append(hours).append("h ")
    if (minutes > 0) stringBuilder.append(minutes).append("m ")
    if (seconds > 0) stringBuilder.append(seconds).append("s ")

    return stringBuilder.toString()
}
fun Int.parsed(): String {
    if (this == 0) return "0s"

    val stringBuilder = StringBuilder()
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    val seconds = this % 60

    if (hours > 0) stringBuilder.append(hours).append("h ")
    if (minutes > 0) stringBuilder.append(minutes).append("m ")
    if (seconds > 0) stringBuilder.append(seconds).append("s ")

    return stringBuilder.toString()
}