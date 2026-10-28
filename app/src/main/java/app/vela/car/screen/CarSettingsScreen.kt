package app.vela.car.screen

import android.content.Context
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.model.Toggle
import app.vela.core.data.RoutingPrefs
import app.vela.offline.OfflineMaps

/**
 * The settings a driver reaches for in the car: spoken directions, the route avoids, traffic
 * and terrain. They write the same prefs the phone's toggles do, so the two always agree.
 * The voice row shows the REAL mode (On / Alerts only / Off, audit 2026-10-28): the old
 * toggle read muted, so an Alerts-only driver saw OFF. Tapping the row steps through the
 * same three modes as the drive's speaker button. Terrain follows the phone's holder and,
 * like the phone, is hidden during navigation by the renderer. The offline row reports the
 * saved-areas count (a viewport-coverage check is not available in a car template).
 * Transit (audit pass 2) now has a car control: the car style carries a
 * vela-transit layer, and this toggle writes the phone's TransitLayer
 * setting, so phone and car agree; the renderer shows it only outside
 * navigation. Places-source still has no car control on purpose — there
 * is no ambient places layer distinct from the basemap POI tiers on the
 * car map, so such a toggle would change nothing there.
 */
class CarSettingsScreen(carContext: CarContext, private val deps: CarDeps) : Screen(carContext) {

    private val prefs = carContext.getSharedPreferences("vela_settings", Context.MODE_PRIVATE)

    // Offline status (audit A-06): the saved-areas count, fetched once per screen creation.
    // Null until the callback lands, so no row is shown rather than a guessed zero.
    private var offlineCount: Int? = null
    private var offlineQueried = false

    override fun onGetTemplate(): Template {
        val list = ItemList.Builder()
            .addItem(voiceModeRow())

            .addItem(
                toggleRow(app.vela.R.string.car_setting_avoid_tolls, RoutingPrefs.avoidTolls) { on ->
                    RoutingPrefs.avoidTolls = on
                    prefs.edit().putBoolean("avoid_tolls", on).apply()
                },
            )
            .addItem(
                toggleRow(app.vela.R.string.car_setting_avoid_highways, RoutingPrefs.avoidHighways) { on ->
                    RoutingPrefs.avoidHighways = on
                    prefs.edit().putBoolean("avoid_highways", on).apply()
                },
            )
            .addItem(
                toggleRow(app.vela.R.string.car_setting_avoid_ferries, RoutingPrefs.avoidFerries) { on ->
                    RoutingPrefs.avoidFerries = on
                    prefs.edit().putBoolean("avoid_ferries", on).apply()
                },
            )
            // The phone's Traffic layer: Google's traffic raster on the car map with no route.
            .addItem(
                toggleRow(app.vela.R.string.car_setting_traffic, app.vela.ui.Traffic.on.value) { on ->
                    app.vela.ui.Traffic.set(carContext, on)
                },
            )
            // Terrain relief, hidden during navigation by the renderer like the phone's.
            .addItem(
                toggleRow(app.vela.R.string.settings_topography, app.vela.ui.Topography.on.value) { on ->
                    app.vela.ui.Topography.set(carContext, on)
                },
            )
            // Transit lines (audit pass 2): writes the phone's TransitLayer
            // setting, so the car map and the phone map always agree. The
            // renderer shows the highlight only outside navigation.
            .addItem(
                toggleRow(app.vela.R.string.settings_transit_layer, app.vela.ui.TransitLayer.on.value) { on ->
                    app.vela.ui.TransitLayer.set(carContext, on)
                },
            )

        // Offline status (audit A-06): saved-areas count, not viewport coverage (a car
        // template cannot check the viewport). Audit R-03: the old "More settings on the
        // phone" row is removed — ListTemplate has no footer slot, and a non-action row
        // reads as a broken button; the phone's settings remain the home for the rest.
        if (!offlineQueried) {
            offlineQueried = true
            OfflineMaps.list(carContext) { regions -> offlineCount = regions.size; invalidate() }
        }
        offlineCount?.let { n ->
            list.addItem(
                Row.Builder()
                    .setTitle(carContext.getString(app.vela.R.string.car_offline_title))
                    .addText(carContext.getString(app.vela.R.string.car_offline_count, n))
                    .build(),
            )
        }
        return ListTemplate.Builder()
            .setTitle(carContext.getString(app.vela.R.string.car_settings))
            .setHeaderAction(Action.BACK)
            .setSingleList(list.build())
            .build()
    }

    /** The voice mode as it really is, not the muted flag: On, Alerts only, or Off. */
    private fun voiceModeRow(): Row {
        val v = deps.voiceGuide
        val mode = when {
            !v.muted -> app.vela.R.string.settings_voice_mode_on
            v.alertsOnly -> app.vela.R.string.settings_voice_mode_alerts
            else -> app.vela.R.string.settings_voice_mode_off
        }
        return Row.Builder()
            .setTitle(carContext.getString(app.vela.R.string.car_setting_voice))
            .addText(carContext.getString(mode))
            .setOnClickListener { app.vela.car.CarVoice.step(carContext, v); invalidate() }
            .build()
    }

    private fun toggleRow(title: Int, checked: Boolean, onChange: (Boolean) -> Unit): Row =
        Row.Builder()
            .setTitle(carContext.getString(title))
            .setToggle(Toggle.Builder { on -> onChange(on); invalidate() }.setChecked(checked).build())
            .build()
}
