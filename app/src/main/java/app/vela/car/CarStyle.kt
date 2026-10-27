package app.vela.car

import org.json.JSONArray
import org.json.JSONObject

/**
 * The layers the phone map adds to Liberty at runtime, written into the car's style JSON before
 * the snapshotter loads it. A `MapSnapshotter` can look a layer up and set its properties, but it
 * cannot add a layer or a source, so what the phone's `ensureLayers` builds with the layer API
 * goes in here as JSON: the flat wetland and plaza twins (parking lots and pedestrian areas, in
 * place of Liberty's patterned fills), the sports field and commercial fills, park trails and bike
 * paths, the style light, and Google's traffic raster, hidden until the renderer shows it.
 * The ids match the phone's, so `applyMapTheme` colors them on both.
 */
internal object CarStyle {
    const val TRAFFIC_SRC = "vela-traffic-src"
    const val TRAFFIC_LAYER = "vela-traffic"
    const val TRAFFIC_OPACITY = 0.6
    const val LIGHT_INTENSITY = 0.25

    private val POLYGON = JSONArray(listOf("match", JSONArray(listOf("geometry-type")), JSONArray(listOf("Polygon", "MultiPolygon")), true, false))
    private val LINE = JSONArray(listOf("match", JSONArray(listOf("geometry-type")), JSONArray(listOf("LineString", "MultiLineString")), true, false))
    private val PATH_WIDTH = JSONArray(listOf("interpolate", JSONArray(listOf("exponential", 1.4)), JSONArray(listOf("zoom")), 14, 0.7, 16, 1.6, 19, 4))

    /** [json] with the car layers in it. The JSON is returned untouched when the style has no
     *  `openmaptiles` source or lacks an anchor layer; the twins then stay absent as they would
     *  on a style without those layers on the phone. With [twins] false (a look Vela does not
     *  paint, where nothing would color them) only the traffic raster goes in. */
    // AutoMap (Build 17): the phone's terrarium-DEM hillshade, as style JSON.
    const val DEM_SRC = "vela-dem"
    const val HILLSHADE_LAYER = "vela-hillshade"
    const val TERRARIUM_TILES = "https://s3.amazonaws.com/elevation-tiles-prod/terrarium/{z}/{x}/{y}.png"
    const val PAINT_SRC = "vela-paint-baked"
    const val PAINT_LAYER = "paint"
    val PAINT_IDS = listOf("median", "surface", "crosswalk", "stop", "bike", "lane", "center", "arrow").map { "vela-paint-$it" }

    /** The painted-roads layers for the car (Build 11): the same 8 layers the phone's
     *  PaintedRoadsLayer builds, as style JSON, because a snapshotter cannot add a layer
     *  or a source after load. Colours here are the DAY values; the renderer re-colours
     *  them per theme after applyMapTheme (it owns the night values). Widths are the
     *  phone's real-metre expressions, translated to JSON verbatim. */
    /** [lightPosition] is the style-light position [radial, azimuthal, polar] for
     *  sun-angle building shading (Build 18); null keeps the style default. */
    fun inject(json: String, trafficTiles: String, twins: Boolean = true, paintDensity: Float = 2.0f, lightPosition: JSONArray? = null): String {
        val style = runCatching { JSONObject(json) }.getOrNull() ?: return json
        val sources = style.optJSONObject("sources") ?: return json
        val src = when {
            sources.has("openmaptiles") -> "openmaptiles"
            else -> sources.keys().asSequence().firstOrNull { sources.optJSONObject(it)?.optString("type") == "vector" } ?: return json
        }
        val layers = style.optJSONArray("layers") ?: return json
        val ids = (0 until layers.length()).map { layers.getJSONObject(it).optString("id") }
        if ("vela-plaza" in ids || (!twins && TRAFFIC_LAYER in ids)) return json
        val out = mutableListOf<JSONObject>()
        for (i in 0 until layers.length()) out += layers.getJSONObject(i)

        fun layer(id: String, type: String, sourceLayer: String, filter: Any?, minzoom: Int?, paint: JSONObject?, layout: JSONObject?): JSONObject =
            JSONObject().apply {
                put("id", id); put("type", type); put("source", src); put("source-layer", sourceLayer)
                if (filter != null) put("filter", filter)
                if (minzoom != null) put("minzoom", minzoom)
                if (paint != null) put("paint", paint)
                if (layout != null) put("layout", layout)
            }
        fun index(id: String) = out.indexOfFirst { it.optString("id") == id }
        fun hide(id: String) {
            val l = out.getOrNull(index(id)) ?: return
            val layout = l.optJSONObject("layout") ?: JSONObject().also { l.put("layout", it) }
            layout.put("visibility", "none")
        }
        fun above(anchor: String, l: JSONObject) { val i = index(anchor); if (i >= 0) out.add(i + 1, l) }
        fun below(anchor: String, l: JSONObject) { val i = index(anchor); if (i >= 0) out.add(i, l) }
        fun classIn(vararg classes: String) = JSONArray(listOf("match", JSONArray(listOf("get", "class")), JSONArray(classes.toList()), true, false))

        // Liberty's patterned fills (a fern hatch, dots) are hidden and flat twins take their
        // place, the same two the phone hides.
        if (twins && "landcover_wetland" in ids) {
            hide("landcover_wetland")
            above("landcover_wetland", layer("vela-wetland", "fill", "landcover", JSONArray(listOf("==", JSONArray(listOf("get", "class")), "wetland")), 12, null, null))
        }
        if (twins && "road_area_pattern" in ids) {
            hide("road_area_pattern")
            above("road_area_pattern", layer("vela-plaza", "fill", "transportation", POLYGON, null, null, null))
        }
        if (twins && "park" in ids) {
            // The phone's order: the pitch twin first, then the commercial twin under it.
            above("park", layer("vela-pitch", "fill", "landuse", classIn("pitch", "playground", "track", "stadium"), 13, null, null))
            above("park", layer("vela-commercial", "fill", "landuse", classIn("commercial", "retail"), 12, null, null))
        }
        if (twins && "road_minor" in ids) {
            val lineLayout = JSONObject().put("line-cap", "round")
            val linePaint = JSONObject().put("line-width", PATH_WIDTH)
            val isPath = JSONArray(listOf("==", JSONArray(listOf("get", "class")), "path"))
            val foot = JSONArray(listOf("match", JSONArray(listOf("get", "subclass")), JSONArray(listOf("path", "bridleway")), true, false))
            val bike = JSONArray(listOf("==", JSONArray(listOf("get", "subclass")), "cycleway"))
            below("road_minor", layer("vela-trails", "line", "transportation", JSONArray(listOf("all", LINE, isPath, foot)), 14, linePaint, lineLayout))
            below("road_minor", layer("vela-bikeroutes", "line", "transportation", JSONArray(listOf("all", LINE, isPath, bike)), 14, linePaint, lineLayout))
        }
        // Painted roads (Build 11): the streamed regional bake, above the last bridge
        // layer like the phone's, below the route and labels. Only for the Vela look.
        if (twins) {
            val paintUrl = runCatching { "pmtiles://" + app.vela.core.config.VelaLinks.paintedRoads() }.getOrNull()
            if (paintUrl != null) {
                sources.put(PAINT_SRC, JSONObject().put("type", "vector").put("url", paintUrl))
                fun metersOf(prop: Any): JSONArray {
                    val lo = 16384.0 / 59960.0; val hi = 4194304.0 / 59960.0
                    return JSONArray(listOf("interpolate", JSONArray(listOf("exponential", 2)), JSONArray(listOf("zoom")),
                        14, JSONArray(listOf("*", prop, lo)), 22, JSONArray(listOf("*", prop, hi))))
                }
                val offW = metersOf(JSONArray(listOf("coalesce", JSONArray(listOf("get", "off")), 0)))
                val widthW = metersOf(JSONArray(listOf("coalesce", JSONArray(listOf("get", "w")), 0)))
                val thin = JSONArray(listOf("interpolate", JSONArray(listOf("linear")), JSONArray(listOf("zoom")),
                    16.5, 0.8, 18, 1.3, 20, 2.4))
                val fadeIn = JSONArray(listOf("interpolate", JSONArray(listOf("linear")), JSONArray(listOf("zoom")),
                    16.5, 0, 17, 1))
                val imageDp = 96.0 / paintDensity.coerceAtLeast(1f)
                fun arrowAt(z: Double) = (8.0 * Math.pow(2.0, z) / 59960.0 / imageDp).toFloat()
                val arrowSize = JSONArray(listOf("interpolate", JSONArray(listOf("exponential", 2)), JSONArray(listOf("zoom")),
                    14, arrowAt(14.0), 22, arrowAt(22.0)))
                fun paintLayer(id: String, type: String, kind: String, paint: JSONObject, layout: JSONObject? = null): JSONObject =
                    JSONObject().apply {
                        put("id", id); put("type", type); put("source", PAINT_SRC); put("source-layer", PAINT_LAYER)
                        put("filter", JSONArray(listOf("==", JSONArray(listOf("get", "k")), kind)))
                        put("minzoom", 16.5); put("paint", paint)
                        if (layout != null) put("layout", layout)
                    }
                fun linePaint(color: String, width: Any, extra: JSONObject.() -> Unit = {}): JSONObject =
                    JSONObject().put("line-color", color).put("line-width", width).apply(extra)
                val paintLayers = listOf(
                    paintLayer("vela-paint-median", "line", "MEDIAN",
                        linePaint("#E4E8EE", widthW),
                        JSONObject().put("line-cap", "round")),
                    paintLayer("vela-paint-surface", "line", "SURFACE",
                        linePaint("#a8b6c4", widthW),
                        JSONObject().put("line-cap", "round").put("line-join", "round")),
                    paintLayer("vela-paint-crosswalk", "line", "CROSSWALK",
                        linePaint("#FFFFFF", metersOf(3.0)) { put("line-dasharray", JSONArray(listOf(0.4, 0.4))); put("line-opacity", fadeIn) }),
                    paintLayer("vela-paint-stop", "line", "STOP",
                        linePaint("#FFFFFF", metersOf(0.6)) { put("line-opacity", fadeIn) }),
                    paintLayer("vela-paint-bike", "line", "BIKE",
                        linePaint("#86CFA0", metersOf(1.2)) { put("line-offset", offW); put("line-opacity", JSONArray(listOf("*", fadeIn, 0.9))) }),
                    paintLayer("vela-paint-lane", "line", "LANE",
                        linePaint("#FFFFFF", thin) { put("line-dasharray", JSONArray(listOf(5, 7))); put("line-offset", offW); put("line-opacity", fadeIn) }),
                    paintLayer("vela-paint-center", "line", "CENTER",
                        linePaint("#F2C200", thin) { put("line-gap-width", thin); put("line-offset", offW); put("line-opacity", fadeIn) }),
                    paintLayer("vela-paint-arrow", "symbol", "ARROW",
                        JSONObject().put("icon-opacity", fadeIn),
                        JSONObject()
                            .put("icon-image", JSONArray(listOf("concat", JSONArray(listOf("get", "icon")), "-l")))
                            .put("icon-rotate", JSONArray(listOf("coalesce", JSONArray(listOf("get", "rot")), 0)))
                            .put("icon-rotation-alignment", "map")
                            .put("icon-pitch-alignment", "map")
                            .put("icon-allow-overlap", true)
                            .put("icon-ignore-placement", true)
                            .put("icon-size", arrowSize)),
                )
                val bridgeIdx = (out.size - 1 downTo 0).firstOrNull { out[it].optString("id").startsWith("bridge_") }
                val at = if (bridgeIdx != null) bridgeIdx + 1 else out.size
                out.addAll(at, paintLayers)
            }
        }
        // Google's traffic raster, where the phone puts it: under the labels, over the buildings.
        sources.put(TRAFFIC_SRC, JSONObject().put("type", "raster").put("tiles", JSONArray(listOf(trafficTiles))).put("tileSize", 256))
        val traffic = JSONObject().apply {
            put("id", TRAFFIC_LAYER); put("type", "raster"); put("source", TRAFFIC_SRC)
            put("layout", JSONObject().put("visibility", "none"))
            put("paint", JSONObject().put("raster-opacity", TRAFFIC_OPACITY).put("raster-fade-duration", 0))
        }
        when {
            "building-3d" in ids -> above("building-3d", traffic)
            "building" in ids -> above("building", traffic)
            else -> out.add(out.indexOfFirst { it.optString("type") == "symbol" }.takeIf { it >= 0 } ?: out.size, traffic)
        }
        // AutoMap (Build 15): overpass/exit emphasis, 2D only — no 3D bridge
        // geometry, which the open stack cannot express (lines extrude nothing).
        // Bridge casings widen x1.3 so an overpass reads against the road below;
        // a dark translucent offset line under the first bridge layer acts as the
        // deck shadow; exit shields grow (icon 1 -> 1.35, text 10 -> 13) whenever
        // they are visible. The Build 5 declutter still hides shields during nav.
        for (l in out) {
            val id = l.optString("id")
            if (id.startsWith("bridge_") && id.endsWith("_casing")) {
                val paint = l.optJSONObject("paint") ?: continue
                if (paint.has("line-width")) {
                    paint.put("line-width", JSONArray(listOf("*", paint.opt("line-width"), 1.3)))
                }
            }
            if (id == "highway-shield-non-us" || id == "highway-shield-us-interstate" || id == "road_shield_us") {
                val layout = l.optJSONObject("layout") ?: continue
                if (layout.has("icon-size")) layout.put("icon-size", 1.35)
                if (layout.has("text-size")) layout.put("text-size", 13)
            }
        }
        val firstBridge = out.indexOfFirst { it.optString("id").startsWith("bridge_") }
        if (firstBridge >= 0) {
            val shadowPaint = JSONObject()
                .put("line-color", "#000000")
                .put("line-opacity", 0.20)
                .put("line-width", JSONArray(listOf("interpolate", JSONArray(listOf("linear")), JSONArray(listOf("zoom")), 12, 3, 16, 8, 20, 18)))
                .put("line-translate", JSONArray(listOf(2.5, 2.5)))
                .put("line-blur", 1)
            out.add(firstBridge, layer("vela-bridge-shadow", "line", "transportation",
                JSONArray(listOf("==", JSONArray(listOf("get", "brunnel")), "bridge")),
                null, shadowPaint, JSONObject().put("line-cap", "round")))
        }
        // AutoMap (Build 16): nearby car parks as nearby places (NOT a parking
        // recommendation — just the tile POIs, shown only on arrival). OMT's poi
        // layer carries the original OSM tag value in `subclass`, so
        // subclass=parking catches amenity=parking points regardless of class.
        // Hidden by default; the renderer shows it inside the arrival window.
        // Polygon-only car parks with no POI point do not appear — that absence
        // is a data limit, not something to fill in by guessing.
        val parkingLayer = layer("vela-parking", "symbol", "poi",
            JSONArray(listOf("==", JSONArray(listOf("get", "subclass")), "parking")),
            15, null,
            JSONObject().put("visibility", "none")
                .put("icon-image", "vela-parking-icon")
                .put("icon-size", 1.0)
                .put("icon-allow-overlap", true)
                .put("icon-ignore-placement", true))
        val firstSymbol = out.indexOfFirst { it.optString("type") == "symbol" }
        if (firstSymbol >= 0) out.add(firstSymbol, parkingLayer) else out.add(parkingLayer)
        // AutoMap (Build 17): terrain relief as shaded relief only (hillshade,
        // NOT 3D terrain). Same DEM + strengths as the phone: below the first
        // road layer so roads + labels stay crisp, capped at z16 so it is
        // regional context and gone at street level, hidden by default — the
        // Topography toggle flips it via the renderer, no style reload.
        sources.put(DEM_SRC, JSONObject().put("type", "raster-dem")
            .put("tiles", JSONArray(listOf(TERRARIUM_TILES))).put("tileSize", 256).put("encoding", "terrarium"))
        val hill = JSONObject().put("id", HILLSHADE_LAYER).put("type", "hillshade").put("source", DEM_SRC)
            .put("maxzoom", 16)
            .put("paint", JSONObject().put("hillshade-exaggeration", 0.32)
                .put("hillshade-shadow-color", "#6b7280")
                .put("hillshade-highlight-color", "#ffffff")
                .put("hillshade-accent-color", "#9aa0a6"))
            .put("layout", JSONObject().put("visibility", "none"))
        val firstRoad = out.indexOfFirst { it.optString("id").startsWith("road") }
        if (firstRoad >= 0) out.add(firstRoad, hill) else out.add(hill)
        // AutoMap (Build 18): sun-angle light position, baked at style load —
        // the snapshotter exposes no light setter, so this only moves when the
        // style reloads (theme flips, surface recreations). Intensity stays at
        // LIGHT_INTENSITY so a low sun never washes anything out, and at night
        // (no position passed) the default light stands untouched.
        if (lightPosition != null) {
            style.put("light", (style.optJSONObject("light") ?: JSONObject()).put("position", lightPosition))
        }
        // The phone's style light, so the car's building tops keep the palette color.
        style.put("light", (style.optJSONObject("light") ?: JSONObject()).put("intensity", LIGHT_INTENSITY))
        style.put("layers", JSONArray(out))
        return style.toString()
    }
}
