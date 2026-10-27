package app.vela.core.location

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

/** The at-rest decision on the accelerometer window (issues #818, #819). */
class StillnessTest {
    private fun window(f: (Int) -> Double): List<Float> = List(Stillness.WINDOW) { f(it).toFloat() }

    @Test fun `a parked phone with an idling engine is still`() {
        assertTrue(Stillness.decide(window { 9.81 + 0.1 * sin(it * 1.3) }))
    }

    @Test fun `a rolling car is not`() {
        assertFalse(Stillness.decide(window { 9.81 + 0.8 * sin(it * 0.9) + if (it % 3 == 0) 0.5 else -0.4 }))
    }

    @Test fun `a window that is not full says nothing`() {
        assertFalse(Stillness.decide(List(Stillness.WINDOW - 1) { 9.81f }))
    }
}
