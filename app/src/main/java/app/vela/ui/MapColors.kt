package app.vela.ui

import android.content.Context
import androidx.compose.runtime.mutableStateOf

/**
 * Process-wide map color-set pick (same reactive-holder shape as AppTheme/UiScale).
 * Three sets ship compiled: "modern" (the AutoMap day/night palettes, sampled from
 * Robert's Google Maps screenshots in Builds 1-2), "classic" (the pre-sample look the
 * project archived in SPEC section 6.2) and "contrast" (a night palette with warm
 * streets on slate land; by day it draws as modern).
 *
 * AutoMap Phase F (2026-10-28): the palettes are LOCKED. Code check before this
 * lock: `applyMapTheme` branched on `classic()`/`contrast()`, so a Classic pick
 * replaced BOTH AutoMap day and night palettes with the archived look, and a
 * Contrast pick replaced the AutoMap night palette — a pick could silently undo
 * the matched look. From Phase F, `current()` is always MODERN, `classic()` and
 * `contrast()` are always false, and `set()` ignores any non-Modern pick, so
 * every reader (applyMapTheme, building overlays, cut veil, road edges) sees the
 * AutoMap palettes and nothing else. The saved `map_palette` pref and the signed
 * calibration bundle's `defaultMapPalette` (pushed by MapViewModel at init) are
 * both ignored for the same reason — a remote fleet default must not re-open
 * the override either. The Automatic/Day/Night scheme (AppTheme.mapMode) and
 * sun-angle shading are untouched: they choose day vs night within Modern,
 * they do not choose a different palette set.
 */
object MapColors {
    const val MODERN = "modern"
    const val CLASSIC = "classic"
    const val CONTRAST = "contrast"

    /** Kept for callers that read the pick state; always MODERN under the lock. */
    val picked = mutableStateOf(MODERN)

    /** Kept for the calibration push path; ignored by [current] under the lock. */
    val remoteDefault = mutableStateOf(MODERN)

    fun init(context: Context) {
        // Deliberately NOT reading the saved pref: a pre-lock Classic/Contrast
        // pick must not survive into the locked build.
        picked.value = MODERN
    }

    fun set(context: Context, palette: String) {
        // Only Modern is a real pick now; anything else is ignored, not stored.
        if (palette != MODERN) return
        picked.value = MODERN
        context.getSharedPreferences("vela_settings", Context.MODE_PRIVATE)
            .edit().putString("map_palette", MODERN).apply()
    }

    /** The palette to draw with right now: always MODERN under the Phase F lock. */
    fun current(): String = MODERN

    fun classic(): Boolean = false
    fun contrast(): Boolean = false
}
