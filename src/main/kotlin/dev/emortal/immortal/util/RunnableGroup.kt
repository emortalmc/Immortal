package dev.emortal.immortal.util

import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArraySet

class RunnableGroup {

    private val runnables = CopyOnWriteArraySet<WeakReference<MinestomRunnable>>()

    fun addRunnable(runnable: MinestomRunnable) {
        runnables.add(WeakReference(runnable))
    }
    fun removeRunnable(runnable: MinestomRunnable) {
        runnables.removeAll { it.get() == null || it.get() == runnable }
    }

    /**
     * Cancels all tasks within the group and clears the group
     */
    fun cancelAll() {
        runnables.forEach {
            it.get()?.cancel()
        }
        runnables.clear()
    }

}