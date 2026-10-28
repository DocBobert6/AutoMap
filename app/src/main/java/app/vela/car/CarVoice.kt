package app.vela.car

import android.content.Context
import app.vela.core.voice.VoiceGuide

/**
 * The car's voice controls. Both write the same two prefs the phone's setting does, so the
 * phone's state follows ([app.vela.ui.map.MapViewModel] listens to them) and the next launch
 * agrees. The car settings switch is on or off ([set]); the drive's speaker button steps through
 * the phone's three modes ([step], issue #842).
 */
object CarVoice {
    /** The drive's speaker button: On, then Alerts only, then Off, then On again, the order of
     *  the phone's button (`MapViewModel.toggleVoice`). */
    fun step(context: Context, voiceGuide: VoiceGuide) = when {
        !voiceGuide.muted -> write(context, voiceGuide, muted = true, alertsOnly = true)
        voiceGuide.alertsOnly -> write(context, voiceGuide, muted = true, alertsOnly = false)
        else -> write(context, voiceGuide, muted = false, alertsOnly = false)
    }

    private fun write(context: Context, voiceGuide: VoiceGuide, muted: Boolean, alertsOnly: Boolean) {
        voiceGuide.alertsOnly = alertsOnly
        voiceGuide.muted = muted
        app.vela.ui.VoiceAlertsOnly.on.value = alertsOnly
        context.getSharedPreferences("vela_settings", Context.MODE_PRIVATE).edit()
            .putBoolean("spoken_directions", !muted)
            .putBoolean("spoken_alerts_only", alertsOnly)
            .apply()
    }

    /** On or off, PRESERVING Alerts only (2026-10-28 settings restructure): the switch flips
     *  only [VoiceGuide.muted] and never touches the alerts-only flag, so a driver in
     *  Alerts only who flips the switch on and back off lands in Alerts only again, not in
     *  full silence. The effective alerts state is muted AND alerts-only, as
     *  [app.vela.ui.map.MapViewModel.setVoiceMode] computes it. */
    fun set(context: Context, voiceGuide: VoiceGuide, on: Boolean) {
        voiceGuide.muted = !on
        val alerts = !on && voiceGuide.alertsOnly
        app.vela.ui.VoiceAlertsOnly.on.value = alerts
        // Audit 2026-10-28: persist the NORMALIZED alerts flag (the same value the
        // in-memory flag and MapViewModel's listener compute), not the raw flag.
        // The in-memory voiceGuide.alertsOnly is deliberately left untouched, so a
        // driver who flips on and back off within a session still lands in
        // Alerts only; on restart the listener normalizes the same way.
        context.getSharedPreferences("vela_settings", Context.MODE_PRIVATE).edit()
            .putBoolean("spoken_directions", on)
            .putBoolean("spoken_alerts_only", alerts)
            .apply()
    }
}
