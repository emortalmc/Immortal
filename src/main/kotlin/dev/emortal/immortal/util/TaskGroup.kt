package dev.emortal.immortal.util

import net.minestom.server.timer.Task
import java.util.concurrent.ConcurrentHashMap

class TaskGroup {

    private val tasks: MutableSet<Task> = ConcurrentHashMap.newKeySet()

    fun addTask(task: Task) = tasks.add(task)
    fun removeTask(task: Task?) = tasks.remove(task)

    fun cancel() {
        tasks.forEach {
            it.cancel()
        }
        tasks.clear()
    }

}