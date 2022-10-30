package dev.emortal.immortal

import dev.emortal.immortal.util.CoroutineRunnable
import dev.emortal.immortal.util.MinestomRunnable
import kotlinx.coroutines.GlobalScope
import net.minestom.server.instance.Instance
import net.minestom.server.timer.Task
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import net.minestom.testing.TestUtils.waitUntilCleared
import org.junit.jupiter.api.Test
import java.lang.ref.WeakReference
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@EnvTest
class RunnableIntegrationTest {

    @Test
    fun minestomRunnableGC(env: Env) {
        var minestomRunnable: MinestomRunnable? = object : MinestomRunnable(delay = Duration.ofSeconds(1), repeat = Duration.ofSeconds(1), iterations = 2, group = null) {
            override fun run() {}
        }
        val ref = WeakReference(minestomRunnable)

        minestomRunnable = null
        waitUntilCleared(ref)
    }

    @Test
    fun coroutineRunnableGC(env: Env) {
        var coroutineRunnable: CoroutineRunnable? = object : CoroutineRunnable(delay = Duration.ofSeconds(1), repeat = Duration.ofSeconds(1), iterations = 2, coroutineScope = GlobalScope) {
            override suspend fun run() {}
        }
        val ref = WeakReference(coroutineRunnable)

        coroutineRunnable!!.cancel()
        coroutineRunnable = null
        waitUntilCleared(ref)
    }

    @Test
    fun coroutineRunnableInstanceGC(env: Env) {
        var instance: Instance? = env.createFlatInstance()
        val instanceRef = WeakReference(instance)

        var runnable: CoroutineRunnable? = object : CoroutineRunnable(delay = Duration.ofSeconds(1), repeat = Duration.ofSeconds(1), iterations = 2, coroutineScope = GlobalScope) {
            override suspend fun run() {
                instance?.resetTitle()
            }
        }

        runnable = null
        env.process().instance().unregisterInstance(instance!!)
        instance = null
        waitUntilCleared(instanceRef)
    }

    @Test
    fun minestomRunnableInstanceGC(env: Env) {
        var instance: Instance? = env.createFlatInstance()
        val instanceRef = WeakReference(instance)

        var runnable: MinestomRunnable? = object : MinestomRunnable(delay = Duration.ofSeconds(1), repeat = Duration.ofSeconds(1), iterations = 20, group = null) {
            override fun run() {
                instance?.resetTitle()
            }
        }
//        runnable = null
        env.process().instance().unregisterInstance(instance!!)
        instance = null
        waitUntilCleared(instanceRef)
    }

    @Test
    fun minestomTaskInstanceGC(env: Env) {
        var instance: Instance? = env.createFlatInstance()
        val instanceRef = WeakReference(instance)

        var task: Task? = env.process().scheduler().buildTask {
            instance?.resetTitle()
        }.repeat(Duration.ofSeconds(1)).schedule()


        task!!.cancel()
        task = null
        env.process().instance().unregisterInstance(instance!!)
        instance = null
        waitUntilCleared(instanceRef)
    }

    @Test
    fun groupRemove(env: Env) {
        var runnable: MinestomRunnable? = object : MinestomRunnable(delay = Duration.ofSeconds(1), repeat = Duration.ofSeconds(1), iterations = 2, group = null) {
            override fun run() {
            }
        }
        var runnable2: MinestomRunnable? = object : MinestomRunnable(delay = Duration.ofSeconds(1), repeat = Duration.ofSeconds(1), iterations = 2, group = null) {
            override fun run() {
            }
        }

        assertEquals(runnable, runnable)
        assertNotEquals(runnable, runnable2)

    }

}