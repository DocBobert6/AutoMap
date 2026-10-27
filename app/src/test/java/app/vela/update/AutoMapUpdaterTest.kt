package app.vela.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoMapUpdaterTest {

    @Test fun nextAlphaIsNewer() {
        assertTrue(AutoMapUpdater.isNewer("0.3.0-alpha.2", "0.3.0-alpha.1"))
    }

    @Test fun firstAlphaBeatsLegacyPlainRelease() {
        // 0.3.0 was the pre-alpha debug versionName, not a stable release.
        assertTrue(AutoMapUpdater.isNewer("0.3.0-alpha.1", "0.3.0"))
    }

    @Test fun plainReleaseBeatsItsOwnPrereleaseOtherwise() {
        assertFalse(AutoMapUpdater.isNewer("0.4.0-alpha.1", "0.4.0"))
    }

    @Test fun nextMinorBeatsAlpha() {
        assertTrue(AutoMapUpdater.isNewer("0.3.1", "0.3.0-alpha.1"))
    }

    @Test fun betaBeatsAlpha() {
        assertTrue(AutoMapUpdater.isNewer("0.3.0-beta.1", "0.3.0-alpha.1"))
    }

    @Test fun stableBeatsBeta() {
        assertTrue(AutoMapUpdater.isNewer("0.3.0", "0.3.0-beta.1"))
    }

    @Test fun reusedDebugTagIsNotNewerThanAlpha() {
        assertFalse(AutoMapUpdater.isNewer("0.3.0-automap-debug", "0.3.0-alpha.1"))
    }

    @Test fun sameVersionIsNotNewer() {
        assertFalse(AutoMapUpdater.isNewer("0.3.0-alpha.1", "0.3.0-alpha.1"))
        assertFalse(AutoMapUpdater.isNewer("v0.3.0-alpha.1", "0.3.0-alpha.1"))
    }

    @Test fun olderVersionIsNotNewer() {
        assertFalse(AutoMapUpdater.isNewer("0.3.0-alpha.1", "0.3.0-alpha.2"))
    }
}
