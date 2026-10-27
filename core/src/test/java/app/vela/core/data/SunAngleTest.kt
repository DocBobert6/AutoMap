package app.vela.core.data

import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/** AutoMap (Build 18): the NOAA approximation must land on known sun positions. */
class SunAngleTest {
    private fun utcMs(y: Int, mo: Int, d: Int, h: Int, mi: Int): Long {
        val c = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        c.set(y, mo - 1, d, h, mi, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    @Test fun summerNoon() {
        val (az, elev) = sunPosition(40.7128, -74.0060, utcMs(2026, 6, 21, 16, 56))
        assertTrue("az $az", az in 170.0..190.0)
        assertTrue("elev $elev", elev in 68.0..76.0)
    }

    @Test fun midnightIsDown() {
        val (_, elev) = sunPosition(40.7128, -74.0060, utcMs(2026, 6, 21, 4, 0))
        assertTrue("elev $elev", elev < 0.0)
    }

    @Test fun winterNoon() {
        val (az, elev) = sunPosition(40.7128, -74.0060, utcMs(2026, 12, 21, 16, 53))
        assertTrue("az $az", az in 170.0..190.0)
        assertTrue("elev $elev", elev in 22.0..30.0)
    }

    @Test fun morningEast() {
        val (az, elev) = sunPosition(40.7128, -74.0060, utcMs(2026, 6, 21, 11, 0))
        assertTrue("az $az", az in 60.0..90.0)
        assertTrue("elev $elev", elev in 10.0..20.0)
    }
}
