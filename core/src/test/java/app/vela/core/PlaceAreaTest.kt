package app.vela.core

import app.vela.core.model.CITY_ZOOM
import app.vela.core.model.COUNTRY_ZOOM
import app.vela.core.model.LatLng
import app.vela.core.model.POSTAL_ZOOM
import app.vela.core.model.Place
import app.vela.core.model.areaZoom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** How an area result is told from a place, from what the keyless search entry carries. */
class PlaceAreaTest {
    private val at = LatLng(38.5449, -121.7405)

    @Test fun `a country or a state has no address and frames wide`() {
        assertEquals(COUNTRY_ZOOM, Place(id = "a", name = "Delaware", location = at).areaZoom()!!, 0.0)
    }

    @Test fun `a city or a county carries only its region as the address`() {
        assertEquals(CITY_ZOOM, Place(id = "b", name = "Sacramento", location = at, address = "California").areaZoom()!!, 0.0)
    }

    @Test fun `a postal code says so`() {
        assertEquals(POSTAL_ZOOM, Place(id = "c", name = "Davis, CA 95616", location = at, address = "Davis, CA", category = "Postal code").areaZoom()!!, 0.0)
    }

    @Test fun `a business, an address and a rated place are places`() {
        assertNull(Place(id = "d", name = "Davis Food Co-op", location = at, address = "620 G St, Davis, CA 95616", category = "Grocery store").areaZoom())
        assertNull(Place(id = "e", name = "615 3rd Street", location = at, address = "615 3rd Street, Davis, CA").areaZoom())
        assertNull(Place(id = "f", name = "Somewhere", location = at, rating = 4.2).areaZoom())
    }
}
