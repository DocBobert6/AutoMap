package app.vela.ui

import android.content.Context
import androidx.compose.runtime.mutableStateOf

/**
 * Map detail: 2D or 3D, the headline control beside Color scheme in Settings > Appearance
 * (mirroring Google's Navigation settings panel). 3D is the default and keeps the existing
 * behaviour — verified in AutoMap Phase F (2026-10-28): the first-install default is
 * already 3D (`getBoolean(KEY, true)` below, and the in-memory default is true), so
 * Phase F required NO code change here; an existing install keeps its saved choice
 * because `init` reads the pref, and 2D remains one tap away in Settings.
 * behaviour: 3D buildings where the Buildings3d toggle allows them, and a tilted car camera.
 * 2D hides the buildings on the phone (via the b3dKeep gate) and flattens the car camera to
 * top-down with the building-3d layer hidden. It never changes routing, traffic, or places.
 */
object MapDetail {
    val threeD = mutableStateOf(true)

    fun init(context: Context) {
        threeD.value = prefs(context).getBoolean(KEY, true)
    }

    fun set(context: Context, value: Boolean) {
        threeD.value = value
        prefs(context).edit().putBoolean(KEY, value).apply()
    }

    private fun prefs(c: Context) = c.getSharedPreferences("vela_settings", Context.MODE_PRIVATE)
    private const val KEY = "map_detail_3d"
}
