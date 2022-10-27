package dev.emortal.immortal.util

import org.tinylog.kotlin.Logger
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * @param delay How long to delay the task from running
 * @param repeat How long between intervals of running
 * @param iterations How many times to iterate, -1 being infinite (also default)
 */
abstract class MinestomRunnable(
    var delay: Duration = Duration.ZERO,
    var repeat: Duration = Duration.ZERO,
    var iterations: Int = -1,
    val executor: ScheduledExecutorService = executorService
) {

    companion object {
        val executorService = Executors.newScheduledThreadPool(2)
    }

    abstract fun run()

    private var future: ScheduledFuture<*>? = null

    var currentIteration = AtomicInteger(0)

    init {

        future = executor.scheduleAtFixedRate({
            try {
                run()
            } catch (e: Throwable) {
                Logger.warn("Timer action failed:")
                e.printStackTrace()
            }

            val currentIter = currentIteration.getAndIncrement()
            if (iterations != -1 && currentIter >= iterations) {
                cancel()
                cancelled()
                return@scheduleAtFixedRate
            }
        }, delay.toMillis(), repeat.toMillis().coerceAtLeast(1), TimeUnit.MILLISECONDS)
    }

    open fun cancelled() {}

    fun cancel() {
        future?.cancel(false)
    }

    fun cancelImmediate() {
//        cancel()
        future?.cancel(true)
    }
}