package dev.emortal.immortal.util

import net.minestom.server.timer.Task
import world.cepi.kstom.Manager
import java.time.Duration

fun task(wait: Duration = Duration.ZERO, repeat: Duration = Duration.ZERO, iterations: Int = 1, finalIteration: () -> Unit = {}, lambda: (Task, Int) -> Unit) {
    lateinit var task1: Task
    var i = 0

    task1 = Manager.scheduler.buildTask {
        if (i >= iterations) {
            task1.cancel()
            finalIteration()
            return@buildTask
        }
        lambda(task1, i);
        i++
    }.delay(wait).repeat(repeat).schedule()
}