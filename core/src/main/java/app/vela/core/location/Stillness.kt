package app.vela.core.location

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Whether the phone is physically at rest, from the accelerometer, for the moments a GPS fix
 * cannot tell a parked car from one creeping along: a wide fix at a red light still reports a
 * speed and a course of its own, which read as 1.5 mph on the tile and a swinging arrow (issues
 * #818, #819). The question is only asked below [SLOW_MPS]; at speed the accelerometer cannot
 * tell a steady cruise from rest and is not asked. [hold] runs the sensor while a drive is on,
 * [release] stops it; the car screen and the phone each hold it for their own drive.
 */
object Stillness : SensorEventListener {
    /** A fix wider than this, under [SLOW_MPS], says nothing about direction or speed. */
    const val ACC_TRUST_M = 30f

    /** Under this speed a wide fix, or a phone at rest, reads as parked. */
    const val SLOW_MPS = 3.0

    /** Samples kept, at about [SAMPLE_MS] apart: 1.5 s of acceleration. */
    const val WINDOW = 15
    const val SAMPLE_MS = 100L

    /** The standard deviation of the acceleration's magnitude below which the phone is at rest.
     *  An idling engine shakes a mounted phone by a tenth or two; a rolling car by half or more. */
    const val STILL_STDDEV = 0.25

    @Volatile var still: Boolean = false
        private set

    private var holds = 0
    private var manager: SensorManager? = null
    private val window = ArrayDeque<Float>()
    private var lastSampleMs = 0L

    @Synchronized fun hold(context: Context) {
        if (holds++ > 0) return
        val sm = context.applicationContext.getSystemService(SensorManager::class.java) ?: return
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        manager = sm
        sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    @Synchronized fun release() {
        if (holds == 0) return
        if (--holds > 0) return
        manager?.unregisterListener(this)
        manager = null
        synchronized(window) { window.clear() }
        still = false
    }

    override fun onSensorChanged(e: SensorEvent) {
        val now = e.timestamp / 1_000_000
        if (now - lastSampleMs < SAMPLE_MS) return
        lastSampleMs = now
        val m = sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2])
        synchronized(window) {
            window.addLast(m)
            while (window.size > WINDOW) window.removeFirst()
            still = decide(window)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /** The decision on one window of acceleration magnitudes: full, and steadier than [STILL_STDDEV]. */
    fun decide(samples: Collection<Float>): Boolean {
        if (samples.size < WINDOW) return false
        val mean = samples.sumOf { it.toDouble() } / samples.size
        val variance = samples.sumOf { (it - mean) * (it - mean) } / samples.size
        return sqrt(variance) < STILL_STDDEV
    }
}
