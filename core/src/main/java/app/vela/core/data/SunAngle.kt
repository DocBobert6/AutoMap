package app.vela.core.data

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * AutoMap (Build 18): the sun's position for building shading, as a NOAA-style
 * approximation (good to ~1 degree — plenty for a light direction, not for
 * astronomy). Pure and JVM-testable: no Android, no clock, no network.
 *
 * Returns (azimuthDeg, elevationDeg): azimuth clockwise from north
 * (0 = north, 90 = east, 180 = south), elevation above the horizon
 * (negative at night). [timeMs] is UTC epoch millis; [lngDeg] is east-positive.
 */
fun sunPosition(latDeg: Double, lngDeg: Double, timeMs: Long): Pair<Double, Double> {
    val jd = timeMs / 86400000.0 + 2440587.5
    val t = (jd - 2451545.0) / 36525.0
    fun mod360(x: Double): Double = ((x % 360.0) + 360.0) % 360.0
    val l0 = mod360(280.46646 + t * (36000.76983 + t * 0.0003032))
    val m = 357.52911 + t * (35999.05029 - t * 0.0001537)
    val e = 0.016708634 - t * (0.000042037 + t * 0.0000001267)
    val mr = m * PI / 180.0
    val c = sin(mr) * (1.914602 - t * (0.004817 + 0.000014 * t)) +
        sin(2 * mr) * (0.019993 - 0.000101 * t) + sin(3 * mr) * 0.000289
    val trueLong = l0 + c
    val omega = 125.04 - 1934.136 * t
    val lambda = trueLong - 0.00569 - 0.00478 * sin(omega * PI / 180.0)
    val eps0 = 23.0 + (26.0 + (21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))) / 60.0) / 60.0
    val eps = eps0 + 0.00256 * cos(omega * PI / 180.0)
    val decl = asin(sin(eps * PI / 180.0) * sin(lambda * PI / 180.0)) * 180.0 / PI
    val y = tan(eps * PI / 360.0).let { it * it }
    val l0r = l0 * PI / 180.0
    val eqTime = 4.0 * (y * sin(2 * l0r) - 2 * e * sin(mr) + 4 * e * y * sin(mr) * cos(2 * l0r) -
        0.5 * y * y * sin(4 * l0r) - 1.25 * e * e * sin(2 * mr)) * 180.0 / PI
    val utcMin = (((timeMs % 86400000) + 86400000) % 86400000) / 60000.0
    var tst = (utcMin + eqTime + 4.0 * lngDeg) % 1440.0
    if (tst < 0) tst += 1440.0
    val ha = tst / 4.0 - 180.0
    val latR = latDeg * PI / 180.0
    val declR = decl * PI / 180.0
    val haR = ha * PI / 180.0
    val elev = asin(sin(latR) * sin(declR) + cos(latR) * cos(declR) * cos(haR)) * 180.0 / PI
    val az = mod360(atan2(sin(haR), cos(haR) * sin(latR) - tan(declR) * cos(latR)) * 180.0 / PI + 180.0)
    return az to elev
}
