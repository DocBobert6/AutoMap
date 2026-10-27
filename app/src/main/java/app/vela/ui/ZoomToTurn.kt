package app.vela.ui

import android.content.Context
import androidx.compose.runtime.mutableStateOf

/** Whether the drive camera zooms out until the next turn sits just under the turn card, and back
 *  in as the car nears it (issue #781, SPEC 4.7 "Framing"). Off by default: it trades the street
 *  names and lights of the close zoom for a view of the stretch ahead. Read per frame by the nav
 *  ticker. */
object ZoomToTurn {
    val on = mutableStateOf(false)
    /** How far out the camera may go to keep the turn on screen: a zoom floor of 12, 10 or 8
     *  ([FLOORS]), or 0 for no pick, when the `turnZoomMin` dial and `ZOOM_TURN_MIN` decide. */
    val floor = mutableStateOf(0.0)

    val FLOORS = listOf(12.0, 10.0, 8.0)

    fun init(context: Context) {
        on.value = prefs(context).getBoolean(KEY, false)
        floor.value = prefs(context).getFloat(FLOOR_KEY, 0f).toDouble()
    }

    fun set(context: Context, value: Boolean) {
        on.value = value
        prefs(context).edit().putBoolean(KEY, value).apply()
    }

    fun setFloor(context: Context, value: Double) {
        floor.value = value
        prefs(context).edit().putFloat(FLOOR_KEY, value.toFloat()).apply()
    }

    private fun prefs(c: Context) = c.getSharedPreferences("vela_settings", Context.MODE_PRIVATE)
    private const val KEY = "zoom_to_turn"
    private const val FLOOR_KEY = "zoom_to_turn_floor"
}
