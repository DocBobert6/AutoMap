package app.vela.car

import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/** The car map's camera arithmetic, free of Android so it can be tested on the JVM. */
internal object CarFraming {
    /** MapLibre's vertical field of view, radians. The camera sits `0.5 * height / tan(FOV / 2)`
     *  pixels from the point it looks at. */
    const val FOV_RAD = 0.6435011087932844

    /** The drive camera's tilt, the phone's. */
    const val NAV_TILT_DEG = 55.0

    /** The following zoom: [ZOOM_NEAR] at rest to [ZOOM_FAR] at [ZOOM_FAR_KMH] and above, a
     *  straight line between. */
    const val ZOOM_NEAR = 17.2
    const val ZOOM_FAR = 15.0
    const val ZOOM_FAR_KMH = 110.0

    fun navZoom(kmh: Double): Double = ZOOM_NEAR - (kmh / ZOOM_FAR_KMH).coerceIn(0.0, 1.0) * (ZOOM_NEAR - ZOOM_FAR)

    /**
     * AutoMap (Build 9) smart auto-zoom: a delta ADDED to the speed zoom (then eased by the
     * ticker's per-tick zoom easing, so it never steps). Inputs are all already known to the
     * car renderer: distance to the next maneuver, that maneuver's type, its lane count
     * where the router supplied lanes, and the leg length to the maneuver after it.
     * Closer / sharper / forkier / tighter sequences zoom IN; many lanes zoom OUT a little
     * so every lane stays in frame. Total clamped to [-0.5, +1.0] so context never
     * overrules the speed zoom or the turn framing by more than a level.
     */
    fun contextZoomDelta(
        distanceToTurnM: Double?,
        maneuver: app.vela.core.model.ManeuverType?,
        laneCount: Int,
        nextLegM: Double?,
    ): Double {
        var d = 0.0
        if (distanceToTurnM != null) {
            d += when {
                distanceToTurnM <= 150.0 -> 0.7
                distanceToTurnM >= 400.0 -> 0.0
                else -> 0.7 * (400.0 - distanceToTurnM) / 250.0
            }
        }
        d += when (maneuver) {
            app.vela.core.model.ManeuverType.FORK_LEFT,
            app.vela.core.model.ManeuverType.FORK_RIGHT,
            app.vela.core.model.ManeuverType.RAMP_LEFT,
            app.vela.core.model.ManeuverType.RAMP_RIGHT,
            app.vela.core.model.ManeuverType.ROUNDABOUT,
            app.vela.core.model.ManeuverType.EXIT_ROUNDABOUT -> 0.4
            app.vela.core.model.ManeuverType.UTURN,
            app.vela.core.model.ManeuverType.SHARP_LEFT,
            app.vela.core.model.ManeuverType.SHARP_RIGHT -> 0.5
            app.vela.core.model.ManeuverType.TURN_LEFT,
            app.vela.core.model.ManeuverType.TURN_RIGHT -> 0.2
            app.vela.core.model.ManeuverType.MERGE,
            app.vela.core.model.ManeuverType.KEEP_LEFT,
            app.vela.core.model.ManeuverType.KEEP_RIGHT,
            app.vela.core.model.ManeuverType.SLIGHT_LEFT,
            app.vela.core.model.ManeuverType.SLIGHT_RIGHT -> 0.15
            else -> 0.0
        }
        d += when {
            laneCount >= 5 -> -0.3
            laneCount == 4 -> -0.15
            else -> 0.0
        }
        if (nextLegM != null) {
            d += when {
                nextLegM < 200.0 -> 0.3
                nextLegM < 500.0 -> 0.15
                else -> 0.0
            }
        }
        return d.coerceIn(-0.5, 1.0)
    }

    /** The camera's distance to the point it looks at, in pixels of a surface [heightPx] tall. */
    fun cameraDistancePx(heightPx: Double): Double = 0.5 * heightPx / tan(FOV_RAD / 2.0)

    /**
     * Where to put the camera's target so that the anchor (the arrow) lands [downPx] below and
     * [rightPx] to the right of the surface's center, in ground pixels at the center's scale:
     * `ahead` along the camera's heading, `left` across it. Flat, that is [downPx] and [rightPx]
     * themselves. Tilted by [tiltDeg], the ground nearer the camera is drawn larger, so fewer
     * ground pixels reach the same screen row, and a lateral offset on that row covers less ground.
     */
    fun targetOffset(downPx: Double, rightPx: Double, tiltDeg: Double, heightPx: Double): Pair<Double, Double> {
        if (tiltDeg <= 0.0 || heightPx <= 0.0) return downPx to rightPx
        val p = Math.toRadians(tiltDeg)
        val d = cameraDistancePx(heightPx)
        // The camera is d·sin p behind the center and d·cos p above it. A ground point g toward
        // the camera sits p − atan((d·sin p − g) / (d·cos p)) below the view axis, and the screen
        // shows that angle at tan(angle)·d pixels. Solved for g at the wanted row.
        val ahead = d * sin(p) - d * cos(p) * tan(p - atan(downPx / d))
        // Depth along the view axis of that row, against the center's depth d.
        val depth = d - ahead * sin(p)
        val left = rightPx * depth / d
        return ahead to left
    }

    /** The least depth along the view axis, as a share of the center's, a ground point is drawn at. */
    const val FRONT_MARGIN = 0.15

    /**
     * How far behind the camera's target a ground point may be and still be drawn, in ground
     * pixels at the center's scale. A point g toward the camera is `d - g·sin p` deep along the
     * view axis ([targetOffset]); at zero it is level with the camera and past that behind it,
     * where the snapshot's projection answers with the point's mirror image through the screen.
     * The limit keeps [FRONT_MARGIN] of the center's depth, which at the drive tilt lands about
     * six screen heights below the center, far off the bottom edge. Flat, every point projects.
     */
    fun behindLimitPx(tiltDeg: Double, heightPx: Double): Double {
        if (tiltDeg <= 0.0 || heightPx <= 0.0) return Double.POSITIVE_INFINITY
        val s = sin(Math.toRadians(tiltDeg))
        if (s < 1e-3) return Double.POSITIVE_INFINITY
        return (1.0 - FRONT_MARGIN) * cameraDistancePx(heightPx) / s
    }

    /** The screen row, below the center, that a ground point [aheadPx] toward the camera lands
     *  on: the forward projection of [targetOffset], for checking it. */
    fun screenDown(aheadPx: Double, tiltDeg: Double, heightPx: Double): Double {
        if (tiltDeg <= 0.0) return aheadPx
        val p = Math.toRadians(tiltDeg)
        val d = cameraDistancePx(heightPx)
        return d * tan(p - atan((d * sin(p) - aheadPx) / (d * cos(p))))
    }
}

/**
 * Which ground points a snapshot taken from a camera at ([lat], [lng]) facing [bearingDeg] can
 * draw: those no more than [limitM] meters behind its target ([CarFraming.behindLimitPx] times
 * the meters in a pixel). The route's driven part runs behind a tilted camera, and drawn from
 * its mirror image it was a gray line swinging about the map (issue 780).
 */
internal class FrontClip(private val lat: Double, private val lng: Double, bearingDeg: Double, private val limitM: Double) {
    private val cosB = cos(Math.toRadians(bearingDeg))
    private val sinB = sin(Math.toRadians(bearingDeg))
    private val cosLat = cos(Math.toRadians(lat))

    /** Meters behind the camera's target along its bearing; negative ahead of it. */
    fun behindM(pLat: Double, pLng: Double): Double {
        var dLng = pLng - lng
        if (dLng > 180.0) dLng -= 360.0 else if (dLng < -180.0) dLng += 360.0
        val north = (pLat - lat) * M_PER_DEG
        val east = dLng * M_PER_DEG * cosLat
        return -(north * cosB + east * sinB)
    }

    fun inFront(pLat: Double, pLng: Double): Boolean = behindM(pLat, pLng) <= limitM

    /** How far along the segment from a to b, 0 to 1, it crosses the limit. For a segment with
     *  one end in front and one behind. */
    fun crossing(aLat: Double, aLng: Double, bLat: Double, bLng: Double): Double {
        val a = behindM(aLat, aLng) - limitM
        val b = behindM(bLat, bLng) - limitM
        return if (a == b) 0.0 else (a / (a - b)).coerceIn(0.0, 1.0)
    }

    private companion object { const val M_PER_DEG = app.vela.core.model.GeoConstants.M_PER_DEG }
}
