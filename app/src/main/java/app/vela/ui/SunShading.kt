package app.vela.ui

import android.content.Context
import androidx.compose.runtime.mutableStateOf

/**
 * AutoMap (Build 18, follow-up 2026-10-26): whether 3D buildings shade by
 * the sun's position. OFF by default (Robert's decision); a first-boot
 * prompt in [Onboarding] asks once whether to enable it as a new feature,
 * and it is flipped any time from Settings > Map. At night the sun is
 * down, so the style keeps its default light and night colours are never
 * washed out. Takes effect on the next style load (the snapshotter cannot
 * move its light after load), which happens on theme flips and surface
 * recreations.
 */
object SunShading {
    val on = mutableStateOf(false)

    fun init(context: Context) {
        on.value = prefs(context).getBoolean(KEY, false)
    }

    fun set(context: Context, value: Boolean) {
        on.value = value
        prefs(context).edit().putBoolean(KEY, value).apply()
    }

    private fun prefs(c: Context) = c.getSharedPreferences("vela_settings", Context.MODE_PRIVATE)
    private const val KEY = "sun_shading_on"
}
