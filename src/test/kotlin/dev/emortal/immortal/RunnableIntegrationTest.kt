package dev.emortal.immortal

import net.minestom.server.instance.Instance
import net.minestom.server.timer.Task
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import net.minestom.testing.TestUtils.waitUntilCleared
import org.junit.jupiter.api.Test
import java.lang.ref.WeakReference
import java.time.Duration

@EnvTest
class RunnableIntegrationTest {

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

}