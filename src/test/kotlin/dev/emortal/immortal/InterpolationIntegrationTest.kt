package dev.emortal.immortal

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class InterpolationIntegrationTest {

    @Test
    fun lerp() {
        assertEquals(0.5f, dev.emortal.immortal.util.lerp(0f, 1f, 0.5f))
        assertEquals(0.7f, dev.emortal.immortal.util.lerp(0f, 1f, 0.7f))
        assertEquals(14f, dev.emortal.immortal.util.lerp(0f, 20f, 0.7f))
        assertEquals(20f, dev.emortal.immortal.util.lerp(20f, 20f, 0.7f))
        assertEquals(154f, dev.emortal.immortal.util.lerp(84f, 154f, 1f))
        assertEquals(84f, dev.emortal.immortal.util.lerp(84f, 154f, 0f))
    }

}