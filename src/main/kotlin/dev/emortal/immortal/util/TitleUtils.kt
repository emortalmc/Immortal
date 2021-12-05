package dev.emortal.immortal.util

import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import net.minestom.server.entity.Player
import net.minestom.server.sound.SoundEvent
import world.cepi.kstom.Manager
import world.cepi.kstom.util.MinestomRunnable
import java.time.Duration
import kotlin.math.roundToInt

fun Iterable<Duration>.sum(): Duration {
    var sum = Duration.ZERO
    for (element in this) {
        if (element == Duration.ZERO) continue
        sum += element
    }
    return sum
}

val Title.Times.totalDuration: Duration
    get() = fadeIn().plus(stay()).plus(fadeOut())

fun Player.queueTitle(title: Title, lambda: () -> Unit = {}) {
    val queue = TitleUtils.playerTitleQueue.getOrDefault(this, mutableListOf())
    val queueDuration = queue.map { (it.times() ?: Title.DEFAULT_TIMES).totalDuration }.sum()

    Manager.scheduler.buildTask {
        this.showTitle(title)
        queue.remove(title)
        lambda.invoke()
    }.delay(queueDuration)
}

fun Player.counterTitle(color: NamedTextColor, game: String, xp: Int) {
    val ticks = 20

    val queue = TitleUtils.playerTitleQueue.getOrDefault(this, mutableListOf())
    val queueDuration = queue.map { (it.times() ?: Title.DEFAULT_TIMES).totalDuration }.sum()

    object : MinestomRunnable() {
        var i = 0
        var currentXp = 0f
        override fun run() {
            if (i > ticks) {
                //player.playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP, Sound.Source.PLAYER, 1f, 1f), Sound.Emitter.self());
                cancel()
                return
            }
            val lastXp = currentXp
            val percentage = i.toFloat() / ticks.toFloat()
            currentXp = expInterp(0f, xp.toFloat(), percentage)

            if (lastXp.roundToInt() == currentXp.roundToInt()) {
                i++
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
                    0.5f + i.toFloat() / 10f
                )
            )
            i++
        }
    }.delay(queueDuration).repeat(Duration.ofMillis(50)).schedule()


}

object TitleUtils {
    val playerTitleQueue = hashMapOf<Player, MutableList<Title>>()
}