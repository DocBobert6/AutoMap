package app.vela.car

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavQueryTest {
    @Test fun `the place name comes out of a navigate intent's query, decoded`() {
        assertEquals("Starbucks", navQueryOf("0,0?q=Starbucks"))
        assertEquals("Pizza place", navQueryOf("0,0?q=Pizza+place&z=10"))
        assertEquals("Pizza place", navQueryOf("0,0?q=Pizza%20place"))
        assertEquals("Davis Food Co-op", navQueryOf("0,0?q=Davis%20Food%20Co-op"))
        assertNull(navQueryOf("38.5449,-121.7405"))
        assertNull(navQueryOf("0,0?q="))
        assertNull(navQueryOf("0,0?q=+"))
    }
}
