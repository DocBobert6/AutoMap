package app.vela.ui

import android.content.Context
import androidx.compose.runtime.mutableStateOf

/**
 * Whether the car screen draws the speed badge (user 2026-10-15): a car has a speedometer, so
 * the badge is clutter there by default and off; the posted limit sign stays, since the car has
 * no such thing. Nested under "Show speed and speed limit", which still governs both.
 */
object CarSpeed {
    val on = mutableStateOf(false)

    fun init(context: Context) {
        on.value = prefs(context).getBoolean(KEY, false)
    }

    fun set(context: Context, value: Boolean) {
        on.value = value
        prefs(context).edit().putBoolean(KEY, value).apply()
    }

    private fun prefs(c: Context) = c.getSharedPreferences("vela_settings", Context.MODE_PRIVATE)
    private const val KEY = "car_speed_on"
}
