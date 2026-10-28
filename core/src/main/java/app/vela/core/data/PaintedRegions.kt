package app.vela.core.data

/**
 * Phase D (2026-10-28): the hosted painted-road region bakes.
 *
 * The phone's baked path ([PaintedRoadsLayer.applyBaked]) and the car's
 * [CarStyle] previously used exactly one streamed bake — California, via
 * `VelaLinks.paintedRoads()`. That is now only the fallback for a view
 * outside every hosted region. When the view/puck is inside a hosted
 * region, exactly ONE regional bake is loaded: the covering region
 * with the lowest [Region.priority], breaking ties by smallest bbox
 * area. Priority outranks area on purpose: Cincinnati metro (priority
 * 1) intentionally overlaps Northern Kentucky (priority 2), and the
 * metro file must win in the overlap because it spans the state line
 * and is denser (32.2% lanes-tagged, 153,621 marks, vs Northern
 * Kentucky's 28.4% and 33,157) — loading both would double-draw every
 * marking in the overlap. No region is ever loaded alongside another.
 *
 * Hosting: the PMTiles files live in this repo under `painted-regions/`
 * on main, served unauthenticated by raw.githubusercontent.com, which
 * answers HTTP Range requests (verified: `Accept-Ranges: bytes` on a
 * raw file), so MapLibre/Vela's streamed-PMTiles reader range-fetches
 * only the visible tiles — the same streaming shape the maxspeed and
 * building overlays already use, but without a time-limited redirect
 * (a release-asset URL redirects to a presigned URL that expires, so
 * it is a poor streaming source). The same list is published as
 * `painted-regions/painted-regions-manifest.json` in the repo, so any
 * client can fetch the manifest; the app compiles this list in so a
 * style can be built synchronously, without stalling a snapshot on a
 * manifest fetch, and the manifest file is the checkable copy of it.
 *
 * Coverage follows OSM tagging and nothing else: a way with no `lanes`
 * tag gets no lane lines, a way with no `turn:lanes` gets no turn
 * arrows. Sparse tagging reads as sparse markings — that absence is
 * reported, never filled in.
 */
object PaintedRegions {
    data class Region(
        val id: String,
        val name: String,
        val url: String,
        val sha256: String,
        val sizeBytes: Long,
        /** Bbox as [south, west, north, east] degrees. */
        val s: Double, val w: Double, val n: Double, val e: Double,
        /** Lower wins in an overlap. */
        val priority: Int,
    ) {
        fun covers(lat: Double, lng: Double) = lat in s..n && lng in w..e
        fun area() = (n - s) * (e - w)
    }

    private const val RAW = "https://raw.githubusercontent.com/DocBobert6/AutoMap/main/painted-regions/"

    val REGIONS: List<Region> = listOf(
        Region(
            id = "cincinnati-metro", name = "Cincinnati metro",
            url = RAW + "painted-cincinnati-metro.pmtiles",
            sha256 = "58c4b0cbef06fea5ff9a75fd8263c99d9db29c792c363568704027131987c7d9",
            sizeBytes = 3924594,
            s = 38.8, w = -84.9, n = 39.5, e = -84.0, priority = 1,
        ),
        Region(
            id = "northern-kentucky", name = "Northern Kentucky",
            url = RAW + "painted-northern-kentucky.pmtiles",
            sha256 = "b11cba8dc778457064dfd52590b495feeccfdd345633838af1b10ade24d49db5",
            sizeBytes = 1377475,
            s = 38.5, w = -85.1, n = 39.1, e = -84.2, priority = 2,
        ),
        Region(
            id = "central-kentucky", name = "Central Kentucky",
            url = RAW + "painted-central-kentucky.pmtiles",
            sha256 = "fe03beb10d51162ad4df8669a95faec3907986d68fff7f2dd4a4b97225611a46",
            sizeBytes = 2468503,
            s = 37.6, w = -85.3, n = 38.5, e = -84.0, priority = 2,
        ),
        // Phase D measured regions (2026-10-28): chosen by measurement
        // over four candidate metros, NOT by proximity to any user —
        // Houston and Dallas-Fort Worth ranked 1st and 2nd on combined
        // lanes + turn:lanes coverage (see BUILD_LOG for the full
        // table). Baked with the same Build 12 pipeline, hosted the
        // same way, priority 2 like the other non-metro regions; their
        // bboxes overlap neither each other nor the Kentucky regions.
        Region(
            id = "houston", name = "Houston",
            url = RAW + "painted-houston.pmtiles",
            sha256 = "eb323b065c8c76eafc49bd20555efc7fcb8721f4d13e3d8a791662b1d473b6a7",
            sizeBytes = 7819391,
            s = 29.50, w = -95.80, n = 30.05, e = -95.00, priority = 2,
        ),
        Region(
            id = "dallas-fort-worth", name = "Dallas-Fort Worth",
            url = RAW + "painted-dallas-fort-worth.pmtiles",
            sha256 = "3dbefc2da75a68bbc41e3dd94a8830d5387c98e7be13342ea93ad9da13b5323b",
            sizeBytes = 10430637,
            s = 32.60, w = -97.40, n = 33.10, e = -96.50, priority = 2,
        ),
    )

    /** The single region covering [lat]/[lng], or null outside all regions. */
    fun pick(lat: Double, lng: Double): Region? =
        REGIONS.filter { it.covers(lat, lng) }
            .minWithOrNull(compareBy({ it.priority }, { it.area() }))

    /** The streamed `pmtiles://` URL for the picked region, or null. */
    fun pickUrl(lat: Double, lng: Double): String? = pick(lat, lng)?.let { "pmtiles://${it.url}" }
}
