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
 * @param group Can be null, however will not be automatically cancelled when games end - caution!
 */
abstract class MinestomRunnable(
    var delay: Duration = Duration.ZERO,
    var repeat: Duration = Duration.ZERO,
    var iterations: Int = -1,
    var group: RunnableGroup?,
    val executor: ScheduledExecutorService = executorService
) {

    companion object {
        val executorService = Executors.newScheduledThreadPool(2)
    }

    abstract fun run()

    private var future: ScheduledFuture<*>? = null

    var currentIteration = AtomicInteger(0)

    init {
        schedule()
    }

    private fun schedule() {
        if (repeat == Duration.ZERO) {
            future = executor.schedule({ run() }, delay.toMillis(), TimeUnit.MILLISECONDS)
        } else {
            future = executor.scheduleAtFixedRate({
                if (iterations != -1 && currentIteration.get() >= iterations) {
                    cancel()
                    cancelled()
                    return@scheduleAtFixedRate
                }

                try {
                    run()
                } catch (e: Throwable) {
                    Logger.warn("Timer action failed:")
                    e.printStackTrace()
                }

                currentIteration.incrementAndGet()
            }, delay.toMillis(), repeat.toMillis().coerceAtLeast(1), TimeUnit.MILLISECONDS)
        }

        group?.addRunnable(this@MinestomRunnable)
    }

    open fun cancelled() {}

    fun cancel() {
        future?.cancel(true)
        future = null
    }

}