package app.vela.car.screen

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import app.vela.core.model.LatLng
import app.vela.core.model.ShortcutKind

/**
 * Every saved place, Home and Work first, then recent places, then saved routes
 * (audit pass 2, 2026-10-28: saved routes ride the same CarDeps store the phone
 * uses — the only CarAppService change is one more injected store passed through,
 * no change to the NAVIGATION category, FEATURE_CLUSTER, or turn publishing).
 * The landing screen shows only six destinations mixed with recents; this is
 * the whole list. A place row previews a route to the place; a route row
 * previews that saved route (its stops for a run, its line-derived vias for
 * a shape — see RoutePreviewCarScreen).
 */
class SavedCarScreen(carContext: CarContext, private val deps: CarDeps) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val places = buildList {
            deps.shortcuts.get(ShortcutKind.HOME)?.let {
                add(Triple(carContext.getString(app.vela.R.string.car_home), it.address, it.location))
            }
            deps.shortcuts.get(ShortcutKind.WORK)?.let {
                add(Triple(carContext.getString(app.vela.R.string.car_work), it.address, it.location))
            }
            deps.savedPlaces.saved().forEach { add(Triple(it.name, it.address, it.location)) }
            deps.recentPlaces.recent().forEach { add(Triple(it.place.name, it.place.address, it.place.location)) }
        }.distinctBy { it.third.lat to it.third.lng }

        // The host says how many rows a list may hold; rows past it are not shown.
        val limit = runCatching {
            carContext.getCarService(ConstraintManager::class.java)
                .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST)
        }.getOrDefault(DEFAULT_LIMIT)

        val list = ItemList.Builder()
        places.take(limit).forEach { (name, address, loc) -> list.addItem(placeRow(name, address, loc)) }
        // Saved routes, after the places: a run previews through its stops, a shape
        // through vias derived from its line (RoutePreviewCarScreen does that work).
        // They share the list because ListTemplate has a single list; the row text
        // marks them as saved routes so they never read as places.
        val routes = runCatching { deps.savedRoutes.all() }.getOrDefault(emptyList())
        routes.forEach { r ->
            list.addItem(
                Row.Builder()
                    .setTitle(r.name)
                    .addText(
                        carContext.getString(app.vela.R.string.car_saved_route) +
                            (r.originLabel.takeIf { it.isNotBlank() }?.let { " · $it" } ?: "") +
                            " → " + r.destLabel.ifBlank { r.name },
                    )
                    .setBrowsable(true)
                    .setOnClickListener {
                        screenManager.push(
                            RoutePreviewCarScreen(carContext, deps, r.destLabel.ifBlank { r.name }, r.dest, r),
                        )
                    }
                    .build(),
            )
        }
        if (places.isEmpty() && routes.isEmpty()) list.setNoItemsMessage(carContext.getString(app.vela.R.string.car_saved_empty))

        return ListTemplate.Builder()
            .setTitle(carContext.getString(app.vela.R.string.car_saved))
            .setHeaderAction(Action.BACK)
            .setSingleList(list.build())
            .build()
    }

    private fun placeRow(name: String, address: String?, dest: LatLng): Row =
        Row.Builder()
            .setTitle(name)
            .apply { address?.takeIf { it.isNotBlank() }?.let { addText(it) } }
            .setBrowsable(true)
            .setOnClickListener { screenManager.push(RoutePreviewCarScreen(carContext, deps, name, dest)) }
            .build()

    private companion object {
        const val DEFAULT_LIMIT = 6
    }
}
