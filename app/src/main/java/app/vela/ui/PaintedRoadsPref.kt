package app.vela.ui

import android.content.Context
import androidx.compose.runtime.mutableStateOf

/**
 * AutoMap (Build 11): the painted-roads feature as a real setting, replacing the
 * developer dial (`debug.vela.tune.paintedRoads`) as the on/off. On by default. The
 * phone's dial still picks the SOURCE (1 = streamed bake, 2 = live build) when on;
 * the car draws the streamed bake whenever this is on. No markings are ever invented:
 * where OSM has no lanes / turn:lanes tags, the bake simply has no marks there.
 */
object PaintedRoadsPref {
    val on = mutableStateOf(true)

    fun init(context: Context) {
        on.value = prefs(context).getBoolean(KEY, true)
    }

    fun set(context: Context, value: Boolean) {
        on.value = value
        prefs(context).edit().putBoolean(KEY, value).apply()
    }

    private fun prefs(c: Context) = c.getSharedPreferences("vela_settings", Context.MODE_PRIVATE)
    private const val KEY = "painted_roads"
}
