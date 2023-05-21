package dev.emortal.immortal

import net.minestom.server.event.player.PlayerMoveEvent
import net.minestom.server.instance.Instance
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import net.minestom.testing.TestUtils.waitUntilCleared
import org.junit.jupiter.api.Test
import java.lang.ref.WeakReference
import java.util.concurrent.CompletableFuture

@EnvTest
class FutureTests {

    @Test
    fun futureTest(env: Env) {
        var instance: Instance? = env.createFlatInstance()
        var future = CompletableFuture.completedFuture(instance)
        val instanceRef = WeakReference(instance)
        val futureRef = WeakReference(future)

        env.process().instance().unregisterInstance(instance!!)

        instance = null
        future = null
        waitUntilCleared(instanceRef)
        waitUntilCleared(futureRef)
    }

    @Test
    fun futureTest2(env: Env) {
        var instance: Instance? = env.createFlatInstance()
        var future: CompletableFuture<Instance>? = CompletableFuture<Instance>()
        future!!.thenAcceptAsync {
            it.eventNode().addListener(PlayerMoveEvent::class.java) {

            }
        }
        future!!.complete(instance)
        val instanceRef = WeakReference(instance)
        val futureRef = WeakReference(future)

        env.process().instance().unregisterInstance(instance!!)

//        instance = null
        future = null
//        waitUntilCleared(instanceRef)
        waitUntilCleared(futureRef)
    }

}