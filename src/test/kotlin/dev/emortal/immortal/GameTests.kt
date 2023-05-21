package dev.emortal.immortal

import net.minestom.server.event.instance.InstanceTickEvent
import net.minestom.server.instance.Instance
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import net.minestom.testing.TestUtils.waitUntilCleared
import org.junit.jupiter.api.Test
import java.lang.ref.WeakReference


@EnvTest
class GameTests {

    @Test
    fun instanceNodeGC(env: Env) {
        class Game(env: Env) {
            var instance: Instance

            init {
                instance = env.process().instance().createInstanceContainer()
                instance.eventNode().addListener(
                    InstanceTickEvent::class.java
                ) { e -> println(instance) }
            }

            fun doNothing() {}
        }

        var game: Game? = Game(env)
        val ref = WeakReference(game)
        env.process().instance().unregisterInstance(game!!.instance)
        game = null
        waitUntilCleared(ref)
    }

}