package dev.emortal.immortal.util

import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import net.minestom.server.entity.Player
import net.minestom.server.sound.SoundEvent
import java.time.Duration
import kotlin.math.roundToInt

fun Player.counterTitle(color: NamedTextColor, game: String, xp: Int) {
    val ticks = 20

    object : MinestomRunnable(repeat = Duration.ofMillis(50), iterations = ticks) {
        var currentXp = 0f
        override fun run() {
            val lastXp = currentXp
            val percentage = currentIteration.toFloat() / ticks.toFloat()
            currentXp = expInterp(0f, xp.toFloat(), percentage)

            if (lastXp.roundToInt() == currentXp.roundToInt()) {
                return
            }

            val actualColor = TextColor.lerp(percentage, NamedTextColor.GRAY, color)

            //progressBar(ceil(percentage * 10) / 10)

            this@counterTitle.showTitle(
                Title.title(
                    Component.text("+" + currentXp.roundToInt(), actualColor, TextDecoration.BOLD),
                    Component.text(game, actualColor),
                    Title.Times.of(Duration.ZERO, Duration.ofSeconds(1), Duration.ofMillis(200))
                )
            )
            this@counterTitle.playSound(
                Sound.sound(
                    SoundEvent.BLOCK_NOTE_BLOCK_HAT,
                    Sound.Source.PLAYER,
                    0.6f,
                    0.5f + currentIteration.toFloat() / 10f
                )
            )
        }
    }
}