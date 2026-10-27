package app.vela.ui

import android.content.Context
import androidx.compose.runtime.mutableStateOf

/**
 * AutoMap (Build 8): fade the 3D buildings as the car approaches a turn, so the turn
 * and the route stay readable through a dense downtown. On by default; the car eases
 * the building-3d opacity toward 0.25 within ~150 m of the next turn and back to full
 * past ~400 m. Per-building occlusion (hiding only the buildings that actually cover
 * the route) is deliberately NOT built: the open style cannot know which buildings
 * occlude the road from the camera, so this is a global fade, as agreed.
 */
object FadeBuildingsOnTurn {
    val on = mutableStateOf(true)

    fun init(context: Context) {
        on.value = prefs(context).getBoolean(KEY, true)
    }

    fun set(context: Context, value: Boolean) {
        on.value = value
        prefs(context).edit().putBoolean(KEY, value).apply()
    }

    private fun prefs(c: Context) = c.getSharedPreferences("vela_settings", Context.MODE_PRIVATE)
    private const val KEY = "fade_buildings_on_turn"
}
