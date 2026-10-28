package app.vela.car.screen

import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import android.text.SpannableString
import android.text.Spanned
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarText
import androidx.car.app.model.Distance
import androidx.car.app.model.DistanceSpan
import androidx.car.app.model.DurationSpan
import androidx.car.app.model.ItemList
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.RoutePreviewNavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import app.vela.car.CarMapRenderer
import app.vela.core.model.LatLng
import app.vela.core.model.Route
import app.vela.core.model.distanceTo
import app.vela.core.model.TravelMode
import app.vela.service.NavigationService
import kotlinx.coroutines.launch

/** Route preview for a chosen destination: fetches routes via [app.vela.core.data.MapDataSource],
 *  lists them with live ETAs on a map showing the route framed, and on "Go" names the picked route
 *  and starts [NavSession]. */
class RoutePreviewCarScreen(
    carContext: CarContext,
    private val deps: CarDeps,
    private val destName: String,
    private val dest: LatLng,
    // Audit pass 2: a saved route opened from SavedCarScreen. A run previews
    // through its stops; a shape previews through vias derived from its line
    // (the phone's SavedRoutes.viasAgainst, same rule) so the preview is the
    // saved way, not just a fresh route to the same destination.
    private val savedRoute: app.vela.core.model.SavedRoute? = null,
) : Screen(carContext), DefaultLifecycleObserver {

    private var routes: List<Route> = emptyList()
    private var selected = 0
    private var loading = true
    private var fetched = false
    private var origin: LatLng? = null
    // The via points the current preview was built through: a run's stops, or
    // a shape's line-derived vias. Reused by the Go-time re-fetch so a moved
    // origin does not silently drop the saved way back to a fresh route.
    private var viaPoints: List<LatLng> = emptyList()
    // Freshness (2026-10-27, parked-origin fix): originFreshAtMs is elapsed-realtime
    // when the origin was verified fresh (live puck or a ≤30 s snapshot); 0 means the
    // origin was never verified fresh and must not be routed from silently.
    private var originFreshAtMs: Long = 0L
    private var staleOrigin = false
    private val freshWindowMs = 30_000L

    init { lifecycle.addObserver(this) }

    override fun onStart(owner: LifecycleOwner) {
        // Shared renderer, preview mode (frame the selected route). Never clear/stop on transitions.
        val renderer = deps.mapRenderer(carContext)
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(renderer)
        renderer.start()
        showSelected(renderer)
    }

    /** The selected route framed in blue, the other listed routes in gray under it. */
    private fun showSelected(renderer: app.vela.car.CarMapRenderer) {
        val shown = routes.take(3)
        renderer.showPreview(shown.getOrNull(selected), shown.filterIndexed { i, _ -> i != selected })
    }

    override fun onGetTemplate(): Template {
        if (!fetched) { fetched = true; fetchRoutes() }

        val builder = RoutePreviewNavigationTemplate.Builder().setHeaderAction(Action.BACK)
        builder.setTitle(destName)

        if (loading) {
            builder.setLoading(true)
            return builder.build()
        }
        if (routes.isEmpty()) {
            // No location / no route found. RoutePreviewNavigationTemplate REQUIRES a navigate action
            // whenever its list is set (non-loading) — a "no items" list with none throws
            // "navigation action cannot be null". So show a MessageTemplate instead of the preview.
            // A stale origin gets its own message: it is NOT a "no route" failure, it is a
            // refusal to route from the parked point (2026-10-27 freshness fix).
            val msg = if (staleOrigin) app.vela.R.string.car_waiting_gps else app.vela.R.string.car_no_route
            return MessageTemplate.Builder(carContext.getString(msg))
                .setTitle(destName)
                .setHeaderAction(Action.BACK)
                .build()
        }

        // RoutePreviewNavigationTemplate caps its route list at 3 — more rows THROW at build.
        val shown = routes.take(3)
        val list = ItemList.Builder()
        shown.forEach { r -> list.addItem(routeRow(r)) }
        list.setOnSelectedListener { idx -> selected = idx; showSelected(deps.mapRenderer(carContext)) }
        list.setSelectedIndex(selected.coerceIn(0, shown.lastIndex))
        builder.setItemList(list.build())
        builder.setNavigateAction(
            Action.Builder()
                .setTitle(carContext.getString(app.vela.R.string.car_go))
                .setOnClickListener { startNav() }
                .build(),
        )
        // Map controls: zoom in on part of the route, then the overview button frames the whole
        // route again (here that is the way back, there is no puck to recenter on).
        val renderer = deps.mapRenderer(carContext)
        builder.setMapActionStrip(
            ActionStrip.Builder()
                .addAction(mapAction(app.vela.R.drawable.ic_car_overview) { showSelected(renderer) })
                .addAction(mapAction(app.vela.R.drawable.ic_car_zoom_in) { renderer.zoomBy(1.0) })
                .addAction(mapAction(app.vela.R.drawable.ic_car_zoom_out) { renderer.zoomBy(-1.0) })
                .build(),
        )
        return builder.build()
    }

    private fun mapAction(iconRes: Int, onClick: () -> Unit): Action =
        Action.Builder()
            .setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, iconRes)).build())
            .setOnClickListener(onClick)
            .build()

    private fun routeRow(r: Route): Row {
        val secs = (r.durationInTrafficSeconds ?: r.durationSeconds).toLong()
        val title = r.summary?.takeIf { it.isNotBlank() } ?: destName
        // The RoutePreviewNavigationTemplate REQUIRES every route row to carry a DurationSpan or
        // DistanceSpan on its title/texts (else "All rows must have either a distance or duration
        // span attached" — a hard crash). Attach both: a DurationSpan and a DistanceSpan, each
        // replacing a placeholder char, so the host renders the ETA + distance in its own format.
        val text = SpannableString("  ·  ")
        text.setSpan(DurationSpan.create(secs), 0, 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        text.setSpan(
            DistanceSpan.create(Distance.create(distanceValue(r.distanceMeters), distanceUnit())),
            text.length - 1, text.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE,
        )
        return Row.Builder()
            .setTitle(CarText.create(title))
            .addText(text)
            .build()
    }

    private fun distanceUnit(): Int =
        if (app.vela.ui.Units.imperial.value) Distance.UNIT_MILES else Distance.UNIT_KILOMETERS

    private fun distanceValue(meters: Double): Double =
        if (app.vela.ui.Units.imperial.value) meters / 1609.344 else meters / 1000.0

    private fun fetchRoutes() {
        // Don't invalidate() here — this runs during onGetTemplate (which already returns the loading
        // template); invalidate mid-build is a no-op the host discourages. Only invalidate on completion.
        loading = true
        staleOrigin = false
        lifecycleScope.launch {
            // Freshness first (2026-10-27, parked-origin fix): prefer the renderer's LIVE
            // puck, fall back to a snapshot only if it is ≤30 s old, and keep waiting for
            // a fresh fix for up to 30 s — polling lastKnown() alone never made it
            // fresher, it just re-read the same parked snapshot. If nothing fresh lands,
            // routes stay empty and the screen shows the waiting-for-GPS message
            // instead of silently routing from the parked point.
            val renderer = deps.mapRenderer(carContext)
            var from: LatLng? = null
            var fresh = false
            var tries = 0
            while (tries < 60) {
                val live = runCatching { renderer.livePuckWithAgeMs() }.getOrNull()
                if (live != null && live.second <= freshWindowMs) { from = live.first; fresh = true; break }
                val known = runCatching { deps.locationProvider.lastKnownWithTime() }.getOrNull()
                if (known != null && known.ageMs != Long.MAX_VALUE && known.ageMs <= freshWindowMs) {
                    from = known.point; fresh = true; break
                }
                kotlinx.coroutines.delay(500)
                tries++
            }
            origin = from
            originFreshAtMs = if (fresh) android.os.SystemClock.elapsedRealtime() else 0L
            staleOrigin = !fresh
            // Saved-route vias first: a run's stops in order; a shape starts
            // direct and gets its vias below, once a reference route exists.
            viaPoints = savedRoute?.takeIf { it.isRun }?.stops?.map { it.location }.orEmpty()
            routes = if (!fresh || from == null) emptyList()
            else runCatching {
                // The avoid settings (car Settings or the phone's chooser); without them a car
                // preview offered toll roads to a driver who had turned tolls off.
                deps.mapDataSource.directions(
                    from, dest, TravelMode.DRIVE, viaPoints,
                    avoidTolls = app.vela.core.data.RoutingPrefs.avoidTolls,
                    avoidHighways = app.vela.core.data.RoutingPrefs.avoidHighways,
                    avoidFerries = app.vela.core.data.RoutingPrefs.avoidFerries,
                )
            }.getOrDefault(emptyList())
            // Shape saved route: derive vias where its line leaves the fresh
            // route's line, re-fetch through them, and lead with that route —
            // the phone's offerSavedRoutes rule, without the phone's chooser.
            if (savedRoute != null && !savedRoute.isRun && routes.isNotEmpty() && from != null) {
                val savedLine = runCatching {
                    app.vela.core.data.google.PolylineCodec.decode(savedRoute.polyline)
                }.getOrDefault(emptyList())
                val vias = if (savedLine.size >= 2) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                        app.vela.core.nav.SavedRoutes.viasAgainst(savedLine, routes.first().polyline)
                    }
                } else emptyList()
                if (vias.isNotEmpty()) {
                    val viaRoutes = runCatching {
                        deps.mapDataSource.directions(
                            from, dest, TravelMode.DRIVE, vias,
                            avoidTolls = app.vela.core.data.RoutingPrefs.avoidTolls,
                            avoidHighways = app.vela.core.data.RoutingPrefs.avoidHighways,
                            avoidFerries = app.vela.core.data.RoutingPrefs.avoidFerries,
                        )
                    }.getOrDefault(emptyList())
                    if (viaRoutes.isNotEmpty()) {
                        viaPoints = vias
                        val mine = viaRoutes.first().copy(detourPlan = vias, savedName = savedRoute.name)
                        routes = listOf(mine) + routes
                        selected = 0
                    }
                }
            }
            loading = false
            invalidate()
            showSelected(deps.mapRenderer(carContext)) // frame the route once fetched
        }
    }

    private fun startNav() {
        val picked = routes.getOrNull(selected) ?: routes.firstOrNull() ?: return
        lifecycleScope.launch {
            // Go-time re-verification (2026-10-27): if the live puck is fresh and has
            // moved > 50 m from the preview origin, re-fetch from the live puck — the
            // preview route was computed from the old point. If the origin was never
            // verified fresh and no fresh live fix exists, refuse to start from it.
            val live = runCatching { deps.mapRenderer(carContext).livePuckWithAgeMs() }.getOrNull()
            val liveFresh = live != null && live.second <= freshWindowMs
            var from = origin
            var routeToStart = picked
            if (liveFresh && from != null && live.first.distanceTo(from) > 50.0) {
                val freshRoutes = runCatching {
                    deps.mapDataSource.directions(
                        live.first, dest, TravelMode.DRIVE, viaPoints,
                        avoidTolls = app.vela.core.data.RoutingPrefs.avoidTolls,
                        avoidHighways = app.vela.core.data.RoutingPrefs.avoidHighways,
                        avoidFerries = app.vela.core.data.RoutingPrefs.avoidFerries,
                    )
                }.getOrDefault(emptyList())
                if (freshRoutes.isNotEmpty()) { from = live.first; routeToStart = freshRoutes.first() }
            }
            if (from == null || (originFreshAtMs == 0L && !liveFresh)) {
                CarToast.makeText(carContext, carContext.getString(app.vela.R.string.car_waiting_gps), CarToast.LENGTH_LONG).show()
                return@launch
            }
            val startFrom = from
            val named = if (routeToStart.provisional) {
                runCatching { deps.mapDataSource.nameRoute(routeToStart, startFrom, dest, TravelMode.DRIVE) }.getOrDefault(routeToStart)
            } else routeToStart
            // Speak through the user's chosen engine, the same mapping as the phone's init path:
            // no pick or a vela.* pick means the in-process Piper voice when it is installed (the
            // service attaches the synth), else the system TTS. Until 2026-09-21 the car always
            // handed nav the system engine and a drive started from the car did not sound like Vela.
            val savedRaw = carContext.applicationContext
                .getSharedPreferences("vela_settings", android.content.Context.MODE_PRIVATE)
                .getString("voice_engine", null)
            val engine = when {
                savedRaw == null || savedRaw.startsWith("vela.") ->
                    if (app.vela.core.voice.VelaPiper.isReady(carContext.applicationContext)) app.vela.core.voice.VelaPiper.ENGINE_ID else null
                else -> savedRaw
            }
            deps.navSession.start(named, dest, destName, engine, emptyList(), TravelMode.DRIVE)
            runCatching { NavigationService.start(carContext.applicationContext) }
            // VelaCarSession opens the drive screen when the session reports the drive.
        }
    }

}
