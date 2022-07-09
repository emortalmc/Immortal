package dev.emortal.immortal.util

import net.minestom.server.timer.ExecutionType
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule
import world.cepi.kstom.Manager
import java.time.Duration

/**
 * A utility to make it easier to build tasks
 *
 * @author emortal
 * @author DasLixou
 */
abstract class MinestomRunnable : Runnable {

    var currentIteration: Long = 0L
    val iterations: Long

    private var task: Task? = null
    private var delaySchedule: TaskSchedule = TaskSchedule.immediate()
    private var repeatSchedule: TaskSchedule = TaskSchedule.stop()
    private var executionType: ExecutionType = ExecutionType.SYNC
    private var taskGroup: TaskGroup? = null

    constructor(delay: Duration = Duration.ZERO, repeat: Duration = Duration.ZERO, executionType: ExecutionType = ExecutionType.SYNC, iterations: Long = -1L, taskGroup: TaskGroup? = null) {
        this.iterations = iterations
        this.taskGroup = taskGroup

        delay(delay)
        repeat(repeat)
        executionType(executionType)
    }

    constructor(delay: TaskSchedule = TaskSchedule.immediate(), repeat: TaskSchedule = TaskSchedule.stop(), executionType: ExecutionType = ExecutionType.SYNC, iterations: Long, taskGroup: TaskGroup? = null) {
        this.iterations = iterations
        this.taskGroup = taskGroup

        delay(delay)
        repeat(repeat)
        executionType(executionType)
    }

    fun delay(duration: Duration) = this.also { delaySchedule = if (duration != Duration.ZERO) TaskSchedule.duration(duration) else TaskSchedule.immediate() }
    fun delay(schedule: TaskSchedule) = this.also { delaySchedule = schedule }

    fun repeat(duration: Duration) = this.also { repeatSchedule = if (duration != Duration.ZERO) TaskSchedule.duration(duration) else TaskSchedule.stop() }
    fun repeat(schedule: TaskSchedule) = this.also { repeatSchedule = schedule }

    fun executionType(type: ExecutionType) = this.also { executionType = type }

    fun schedule(): Task {
        val t = Manager.scheduler.buildTask {
            this.run()

            currentIteration++
            if (iterations != -1L && currentIteration >= iterations) {
                cancel()
                cancelled()
                return@buildTask
            }
        }
            .delay(delaySchedule)
            .repeat(repeatSchedule)
            .executionType(executionType)
            .schedule()

        taskGroup?.tasks?.add(t)

        this.task = t
        return t
    }

    fun cancel() = task?.cancel()

    /**
        Called when iterations run out
     */
    open fun cancelled() {}
}