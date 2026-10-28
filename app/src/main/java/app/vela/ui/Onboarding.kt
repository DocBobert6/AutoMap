package app.vela.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import app.vela.core.voice.VelaPiper

/**
 * First-run welcome state (audit R-09, 2026-10-28): the old header described a
 * donation prompt and a permanent "Support Vela" Settings entry, neither of which
 * exists in AutoMap — support was removed in Build 0 and the credit lives in
 * About. What this object actually owns is the first-run prompt chain (location,
 * notification, voice, sun-shading), each asked once and never blocking.
 *
 * Process-wide reactive holder, same shape as [Units]/[Traffic]; `init()`-ed in
 * `VelaApp`, persisted to `vela_onboarding`.
 */
object Onboarding {
    /** False until the user has seen the welcome screen once. */
    val welcomeDone = mutableStateOf(true)

    /** True for the single session where the one-time location prompt should fire. The
     *  first-run flow fires the system permission dialog directly (there is no separate
     *  rationale screen in the current flow — the old docstring claiming one was stale);
     *  suppressed once location is granted or the user has answered. A denial still leaves
     *  search/browse working, and the locate button re-asks later. */
    val showLocationPrompt = mutableStateOf(false)

    /** True for the single session where the one-time notification ask should fire — right after the
     *  location step, because turn-by-turn keeps your next turn in a notification and asking during
     *  setup means the first navigation just works. Android 13+ only; suppressed once granted or
     *  answered. A denial gets one plain-words are-you-sure in VelaRoot, then the app moves on. */
    val showNotifPrompt = mutableStateOf(false)

    /** True for the single session where the one-time "download the AutoMap neural voice?" prompt should
     *  show — offered right after the notification step so the best voice is one tap away. Suppressed
     *  once the model is present or the user has answered. */
    val showVoicePrompt = mutableStateOf(false)

    /** AutoMap follow-up 2026-10-26: one-time first-boot offer of sun-angle
     *  building shading as a new feature. SunShading defaults OFF; this
     *  prompt is the only thing that can turn it on at first boot, and
     *  only if the user says yes. Suppressed once answered, either way,
     *  and changeable any time in Settings > Map. */
    val showSunShadingPrompt = mutableStateOf(false)

    private const val PREFS = "vela_onboarding"
    private const val WEEK_MS = 7L * 24 * 60 * 60 * 1000

    fun init(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        welcomeDone.value = p.getBoolean("welcome_done", false)
        var firstMs = p.getLong("first_ms", 0L)
        if (firstMs == 0L) {
            firstMs = System.currentTimeMillis()
            p.edit().putLong("first_ms", firstMs).apply()
        }
        // Location rationale: show once, unless already granted or answered. Granted counts as done
        // (never nag someone who allowed it). On a brand-new install welcomeDone is still false here →
        // completeWelcome arms it right after the welcome screen instead.
        val locationPromptDone = p.getBoolean("location_prompt_done", false)
        showLocationPrompt.value = welcomeDone.value && !locationPromptDone && !hasLocation(context)

        // Voice prompt: offer the neural voice once, unless it's already downloaded or answered. On a
        // brand-new install welcomeDone is still false here (welcome not seen yet) → completeWelcome
        // arms the location step, whose dismissal arms this one.
        val voicePromptDone = p.getBoolean("voice_prompt_done", false)
        showVoicePrompt.value = welcomeDone.value && !voicePromptDone && !VelaPiper.isReady(context)
        // Sun-shading prompt for existing installs upgrading to this build:
        // if onboarding is otherwise finished and the sun question was never
        // answered, ask it once now (it is the first boot of this feature).
        // It must not stack on top of an unanswered voice prompt.
        if (welcomeDone.value && !showVoicePrompt.value && !showLocationPrompt.value &&
            !p.getBoolean("sun_prompt_done", false)
        ) {
            showSunShadingPrompt.value = true
        }
    }

    /** After the voice step: show the voice offer if it is still owed,
     *  otherwise arm the one-time sun-shading offer if it is still owed. */
    private fun armVoiceOrSun(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.getBoolean("voice_prompt_done", false) && !VelaPiper.isReady(context)) {
            showVoicePrompt.value = true
        } else if (!p.getBoolean("sun_prompt_done", false)) {
            showSunShadingPrompt.value = true
        }
    }

    private fun hasLocation(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Notifications need no runtime grant before Android 13. */
    private fun hasNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** Mark the location step handled (whatever the user chose) so it never shows again, and arm the
     *  next step: notifications where the OS needs an ask, else straight to the voice offer. */
    fun dismissLocationPrompt(context: Context) {
        showLocationPrompt.value = false
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        p.edit().putBoolean("location_prompt_done", true).apply()
        if (!p.getBoolean("notif_prompt_done", false) && !hasNotifications(context)) {
            showNotifPrompt.value = true
        } else {
            armVoiceOrSun(context)
        }
    }

    /** Mark the notification step handled so it never shows again, and arm the voice offer next. */
    fun dismissNotifPrompt(context: Context) {
        showNotifPrompt.value = false
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        p.edit().putBoolean("notif_prompt_done", true).apply()
        armVoiceOrSun(context)
    }

    /** Mark the voice prompt handled so it never shows again, then arm the
     *  sun-shading offer (the new last onboarding step). */
    fun dismissVoicePrompt(context: Context) {
        showVoicePrompt.value = false
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean("voice_prompt_done", true).apply()
        armVoiceOrSun(context)
    }

    /** Answer the one-time sun-shading offer, once, either way. Only an
     *  explicit yes enables it; a skip or dismiss leaves it OFF, as
     *  defaulted. Changeable any time in Settings > Map. */
    fun dismissSunShadingPrompt(context: Context, enable: Boolean) {
        showSunShadingPrompt.value = false
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean("sun_prompt_done", true).apply()
        SunShading.set(context, enable)
    }

    fun completeWelcome(context: Context) {
        welcomeDone.value = true
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        p.edit().putBoolean("welcome_done", true).apply()
        // First-run order: location → notifications → voice. Arm location here; each step's
        // dismissal arms the next. If a step is somehow already granted/answered, skip past it.
        if (!p.getBoolean("location_prompt_done", false) && !hasLocation(context)) {
            showLocationPrompt.value = true
        } else if (!p.getBoolean("notif_prompt_done", false) && !hasNotifications(context)) {
            showNotifPrompt.value = true
        } else {
            armVoiceOrSun(context)
        }
    }

}
