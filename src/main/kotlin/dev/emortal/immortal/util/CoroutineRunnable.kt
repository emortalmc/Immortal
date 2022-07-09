package dev.emortal.immortal.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.tinylog.kotlin.Logger
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Creates a task using Kotlin coroutines instead of Minestom
 * May allow for extra accuracy although higher overhead
 * Prefer MinestomRunnable
 *
 * @param delay How long to delay the task from running
 * @param repeat How long between intervals of running
 * @param iterations How many times to iterate, -1 being infinite (also default)
 * @param coroutineScope The coroutine scope to use, should be set to the game's scope if created inside a game as the tasks will be automatically cancelled on end, use GlobalScope otherwise
 */
abstract class CoroutineRunnable(
    var delay: Duration = Duration.ZERO,
    var repeat: Duration = Duration.ZERO,
    var iterations: Int = -1,
    val coroutineScope: CoroutineScope
) {

    abstract suspend fun run()

    private val keepRunning = AtomicBoolean(true)
    private var job: Job? = null
    private val tryRun = suspend {
        try {
            run()
        } catch (e: Throwable) {
            Logger.warn("Timer action failed:")
            e.printStackTrace()
        }
    }

    var currentIteration = AtomicInteger(0)

    init {

        job = coroutineScope.launch {
            delay(delay.toMillis())
            if (repeat.toMillis() != 0L) {
                while (keepRunning.get()) {
                    tryRun()
                    delay(repeat.toMillis())
                    val currentIter = currentIteration.incrementAndGet()
                    if (iterations != -1 && currentIter >= iterations) {
                        cancel()
                        cancelled()
                        return@launch
                    }
                }
            } else {
                if (keepRunning.get()) {
                    tryRun()
                }
            }
        }
    }

    open fun cancelled() {}

    fun cancel() {
        keepRunning.set(false)
    }

    fun cancelImmediate() {
        cancel()
        job?.cancel()
    }
}