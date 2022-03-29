package dev.emortal.immortal.util

import java.time.Duration

val Long.millis get() = Duration.ofMillis(this)
val Long.seconds get() = Duration.ofSeconds(this)
val Long.minutes get() = Duration.ofMinutes(this)
val Long.hours get() = Duration.ofHours(this)
val Long.days get() = Duration.ofDays(this)