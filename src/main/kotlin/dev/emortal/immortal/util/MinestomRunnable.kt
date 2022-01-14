package dev.emortal.immortal.util

import java.time.Duration
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

abstract class MinestomRunnable(val delay: Duration = Duration.ZERO, val repeat: Duration = Duration.ZERO, var iterations: Int = -1, timer: Timer = defaultTimer) {

    companion object {
        val defaultTimer = Timer()
    }

    var currentIteration = 0
    private val task: TimerTask = timer.scheduleAtFixedRate(delay.toMillis(), repeat.toMillis()) {
        this@MinestomRunnable.run()
        currentIteration++

        if (iterations != -1 && currentIteration >= iterations) {
            this@MinestomRunnable.cancel()
        }
    }

    abstract fun run()
    open fun cancelled() {}

    fun cancel() {
        cancelled()
        task.cancel()
    }
}