package dev.emortal.immortal

import dev.emortal.immortal.util.CoroutineRunnable
import dev.emortal.immortal.util.MinestomRunnable
import kotlinx.coroutines.GlobalScope
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import net.minestom.testing.TestUtils.waitUntilCleared
import org.junit.jupiter.api.Test
import java.lang.ref.WeakReference
import java.time.Duration

@EnvTest
class RunnableIntegrationTest {

    @Test
    fun minestomRunnableGC(env: Env) {
        var minestomRunnable: MinestomRunnable? = object : MinestomRunnable(delay = Duration.ofSeconds(1), repeat = Duration.ofSeconds(1), iterations = 2) {
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

        coroutineRunnable = null
        waitUntilCleared(ref)
    }

}