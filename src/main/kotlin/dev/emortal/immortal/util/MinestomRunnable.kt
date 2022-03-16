package dev.emortal.immortal.util

import world.cepi.kstom.Manager
import java.time.Duration
import java.util.*
import kotlin.concurrent.schedule
import kotlin.concurrent.scheduleAtFixedRate

/**
 * Creates a task using Java's scheduler instead of Minestom's
 * Allows for more accuracy, and appears to run faster
 *
 * @param delay How long to delay the task from running
 * @param repeat How long between intervals of running
 * @param iterations How many times to iterate, -1 being infinite (also default)
 * @param timer Custom timer to use, should be set to the game's timer if created inside a game as the tasks will automatically cancel, use MinestomRunnable.defaultTimer otherwise
 */
abstract class MinestomRunnable(val delay: Duration = Duration.ZERO, val repeat: Duration = Duration.ZERO, var iterations: Int = -1, timer: Timer) {

    companion object {
        val defaultTimer = Timer()
    }

    private var task: TimerTask? = null
    var currentIteration = 0

    init {

        try {
            if (repeat.toMillis() == 0L) {
                if (delay.toMillis() != 0L) timer.schedule(delay.toMillis()) { this@MinestomRunnable.run() }
            }

            if (iterations < 2 && delay.toMillis() == 0L && repeat.toMillis() == 0L) {
                this@MinestomRunnable.run()
                this@MinestomRunnable.cancel()
            } else {
                task = timer.scheduleAtFixedRate(delay.toMillis(), repeat.toMillis()) {
                    if (iterations != -1 && currentIteration >= iterations) {
                        this@MinestomRunnable.cancel()
                        cancelled()
                        return@scheduleAtFixedRate
                    }

                    this@MinestomRunnable.run()
                    currentIteration++
                }
            }
        } catch (e: Exception) {
            Manager.exception.handleException(e)
        }
    }

    abstract fun run()
    open fun cancelled() {}

    fun cancel() {
        task?.cancel()
    }
}