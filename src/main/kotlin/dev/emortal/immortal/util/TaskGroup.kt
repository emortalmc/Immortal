package dev.emortal.immortal.util

import net.minestom.server.timer.Task
import java.util.concurrent.ConcurrentHashMap

class TaskGroup {

    val tasks: MutableSet<Task> = ConcurrentHashMap.newKeySet()

    fun cancel() {
        tasks.forEach {
            it.cancel()
        }
        tasks.clear()
    }

}