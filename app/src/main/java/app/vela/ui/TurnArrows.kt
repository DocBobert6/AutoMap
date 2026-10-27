package app.vela.ui

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import app.vela.core.model.Route
import app.vela.core.nav.TurnArrowGeometry

/**
 * The arrows drawn on the route at its turns while navigating (issue #804): whether they are
 * wanted (Settings > Navigation, on by default) and the arrows for the drive in hand, from the
 * step in hand onward. `NavController` writes [update] per nav tick, change-gated on the route
 * and the step; `VelaMapView` reads [arrows] directly, so `MapScreen` carries no new parameter.
 */
object TurnArrows {
    val on = mutableStateOf(true)
    val arrows = mutableStateOf<List<TurnArrowGeometry.Arrow>>(emptyList())

    private var route: Route? = null
    private var step = -1

    fun init(context: Context) {
        on.value = prefs(context).getBoolean(KEY, true)
    }

    fun set(context: Context, value: Boolean) {
        on.value = value
        prefs(context).edit().putBoolean(KEY, value).apply()
        route = null // recomputed on the next tick
    }

    fun update(route: Route?, stepIndex: Int) {
        if (route === this.route && stepIndex == step) return
        this.route = route
        step = stepIndex
        arrows.value = if (route == null || !on.value) emptyList() else TurnArrowGeometry.arrows(route, stepIndex)
    }

    private fun prefs(c: Context) = c.getSharedPreferences("vela_settings", Context.MODE_PRIVATE)
    private const val KEY = "turn_arrows_on"
}
