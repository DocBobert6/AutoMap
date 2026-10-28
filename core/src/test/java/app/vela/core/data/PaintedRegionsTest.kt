package app.vela.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaintedRegionsTest {
    @Test fun overlapPicksMetroNotNorthernKentucky() {
        // Covington, KY (39.08, -84.51) is inside both Cincinnati metro
        // and Northern Kentucky; the metro file must win, not both.
        assertEquals("cincinnati-metro", PaintedRegions.pick(39.08, -84.51)?.id)
    }

    @Test fun northernKentuckyOutsideMetroPicksNorthern() {
        // Dry Ridge, KY (38.68, -84.53) is south of the metro bbox.
        assertEquals("northern-kentucky", PaintedRegions.pick(38.68, -84.53)?.id)
    }

    @Test fun centralKentuckyPicksCentral() {
        assertEquals("central-kentucky", PaintedRegions.pick(38.04, -84.50)?.id)
    }

    @Test fun outsideAllRegionsPicksNothing() {
        assertNull(PaintedRegions.pick(40.71, -74.00))
        assertNull(PaintedRegions.pickUrl(40.71, -74.00))
    }

    @Test fun pickUrlIsStreamedPmtiles() {
        assertEquals(
            "pmtiles://https://raw.githubusercontent.com/DocBobert6/AutoMap/main/painted-regions/painted-cincinnati-metro.pmtiles",
            PaintedRegions.pickUrl(39.10, -84.51),
        )
    }

    @Test fun everyRegionHasUrlShaAndSize() {
        for (r in PaintedRegions.REGIONS) {
            assertEquals(64, r.sha256.length)
            check(r.sizeBytes > 0) { r.id }
            check(r.url.endsWith(".pmtiles")) { r.id }
        }
    }
}
