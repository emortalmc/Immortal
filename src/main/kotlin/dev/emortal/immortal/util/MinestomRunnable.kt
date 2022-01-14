package dev.emortal.immortal.util

import java.time.Duration
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

class MinestomRunnable(val delay: Duration = Duration.ZERO, val repeat: Duration = Duration.ZERO, var iterations: Int = -1, timer: Timer = defaultTimer, val run: (MinestomRunnable) -> Unit) {

    companion object {
        val defaultTimer = Timer()
    }

    var currentIteration = 0
    private val task: TimerTask = timer.scheduleAtFixedRate(delay.toMillis(), repeat.toMillis()) {
        run(this@MinestomRunnable)
        currentIteration++

        if (iterations != -1 && currentIteration >= iterations) {
            this@MinestomRunnable.cancel()
        }
    }

    var onCancel = { }

    fun onCancel(lambda: () -> Unit) {
        onCancel = lambda
    }

    fun cancel() {
        onCancel()
        task.cancel()
    }
}