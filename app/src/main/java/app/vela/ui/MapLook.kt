package app.vela.ui

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import app.vela.core.data.tiles.MapStyle

/**
 * Settings > Appearance > Map look (pref `map_look`, default the Vela look on Liberty): which
 * OpenFreeMap style the basemap loads (issue #750). A process-wide holder like [MapColors]. A
 * change reloads the map through `styleKey`, and asks `MapFonts` for that look's patched style.
 *
 * AutoMap Phase F (2026-10-28): the look is LOCKED to Liberty, the painted look
 * the AutoMap palettes are applied to. Code check before this lock: the other
 * looks (Bright, Positron, Fiord, Black) load as OpenFreeMap serves them and
 * `applyMapTheme` is gated on `painted`, so picking one replaced the whole
 * basemap — AutoMap day/night palettes, road widths, icons and twin fills
 * included — with a served style's own colours. From Phase F, `init` ignores
 * any saved non-Liberty pref and `set` ignores any non-Liberty pick, so the
 * AutoMap palettes cannot be overridden by a look pick either. The layers
 * sheet's look tiles route through the same `set`, so they are covered too.
 * Satellite (a separate tile layer over the look) is untouched.
 */
object MapLook {
    val style = mutableStateOf(MapStyle.DEFAULT)

    fun init(context: Context) {
        // Deliberately NOT reading the saved pref: a pre-lock non-Liberty
        // pick must not survive into the locked build.
        style.value = MapStyle.DEFAULT
    }

    fun set(context: Context, look: MapStyle) {
        // Only Liberty (the DEFAULT) is a real pick now; anything else is ignored.
        if (look != MapStyle.DEFAULT) return
        style.value = look
        prefs(context).edit().putString(KEY, look.key).apply()
        app.vela.ui.map.MapFonts.ensure(context, look)
    }

    private fun prefs(c: Context) = c.getSharedPreferences("vela_settings", Context.MODE_PRIVATE)
    private const val KEY = "map_look"
}
