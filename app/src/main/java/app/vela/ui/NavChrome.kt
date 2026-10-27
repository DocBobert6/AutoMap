package app.vela.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat

/** Where the name of the road you are driving is shown during navigation (issue #288 asked for
 *  it under the arrow; that pill cannot be centered for long names because it is pinned to the
 *  arrow, so the default is Google's fixed spot above the bottom bar). Values: "off", "bar", "puck". */
object RoadLabel {
    const val OFF = "off"
    const val BAR = "bar"
    const val PUCK = "puck"
    /** Inside the nav bar, where the lift chevron sits (issue #553). */
    const val IN_BAR = "inbar"
    val mode = mutableStateOf(PUCK)

    fun init(context: Context) {
        mode.value = prefs(context).getString(KEY, PUCK) ?: PUCK
    }

    fun set(context: Context, value: String) {
        mode.value = value
        prefs(context).edit().putString(KEY, value).apply()
    }

    private fun prefs(c: Context) = c.getSharedPreferences("vela_settings", Context.MODE_PRIVATE)
    private const val KEY = "road_label"
}

/** The navigation arrow's size and colors (issue #344): three sizes for eyes that want a bigger
 *  target, and a white-disc variant so the puck does not blend into the blue route line. Both the
 *  follow-mode Compose overlay and the map symbol draw from the same bitmap, so one holder feeds
 *  both; the map style key carries [key] so a change re-registers the symbol image. */
object PuckStyle {
    const val SIZE_NORMAL = "normal"
    const val SIZE_LARGE = "large"
    const val SIZE_XL = "xl"
    const val STYLE_BLUE = "blue"
    const val STYLE_WHITE = "white"
    val size = mutableStateOf(SIZE_NORMAL)
    val style = mutableStateOf(STYLE_BLUE)

    fun scale(): Float = when (size.value) {
        SIZE_LARGE -> 1.25f
        SIZE_XL -> 1.5f
        else -> 1f
    }
    fun whiteDisc(): Boolean = style.value == STYLE_WHITE
    fun key(): String = "${size.value}/${style.value}/${shape.value}/${carColor.value}"

    /** The nav icon (discussion #611): the arrow, or a top-down car in [carColor].
     *  AutoMap 2026-10-26: [carColor] is a hex colour (#RRGGBB), chosen in Settings with a
     *  wheel, sliders, or a hex field. A saved legacy name (red/blue/white/green/yellow)
     *  maps to its exact old hex, so a user who never opens the picker keeps their colour. */
    const val SHAPE_ARROW = "arrow"
    const val SHAPE_CAR = "car"
    const val SHAPE_UFO = "ufo"
    const val SHAPE_SHIP = "ship"
    const val SHAPE_DUCK = "duck"
    const val DEFAULT_CAR_COLOR = "#FFFFFF" // AutoMap 2026-10-27: white is the first-install default; a saved choice still wins
    private val LEGACY_CAR_COLORS = mapOf(
        "red" to "#D93025", "blue" to "#1A46E5", "white" to "#F4F5F7",
        "green" to "#1E9E5A", "yellow" to "#F2C230",
    )
    val shape = mutableStateOf(SHAPE_ARROW)
    val carColor = mutableStateOf(DEFAULT_CAR_COLOR)
    fun isCar(): Boolean = shape.value == SHAPE_CAR

    /** Normalise a hex string or a legacy colour name to #RRGGBB, or null if invalid. */
    fun normalizeCarColor(value: String?): String? {
        if (value == null) return null
        val hex = value.trim()
        if (Regex("^#[0-9A-Fa-f]{6}$").matches(hex)) return hex.uppercase(java.util.Locale.US)
        return LEGACY_CAR_COLORS[hex.lowercase(java.util.Locale.US)]
    }

    fun setShape(context: Context, value: String) {
        shape.value = value
        prefs(context).edit().putString(KEY_SHAPE, value).apply()
    }

    fun setCarColor(context: Context, value: String) {
        val hex = normalizeCarColor(value) ?: return
        carColor.value = hex
        prefs(context).edit().putString(KEY_CAR_COLOR, hex).apply()
    }

    fun init(context: Context) {
        val p = prefs(context)
        size.value = p.getString(KEY_SIZE, SIZE_NORMAL) ?: SIZE_NORMAL
        style.value = p.getString(KEY_STYLE, STYLE_BLUE) ?: STYLE_BLUE
        shape.value = p.getString(KEY_SHAPE, SHAPE_CAR) ?: SHAPE_CAR // AutoMap: the car is the default puck, not the blue arrow (a saved choice still wins)
        carColor.value = normalizeCarColor(p.getString(KEY_CAR_COLOR, DEFAULT_CAR_COLOR)) ?: DEFAULT_CAR_COLOR
    }

    fun setSize(context: Context, value: String) {
        size.value = value
        prefs(context).edit().putString(KEY_SIZE, value).apply()
    }

    fun setStyle(context: Context, value: String) {
        style.value = value
        prefs(context).edit().putString(KEY_STYLE, value).apply()
    }

    private fun prefs(c: Context) = c.getSharedPreferences("vela_settings", Context.MODE_PRIVATE)
    private const val KEY_SIZE = "puck_size"
    private const val KEY_STYLE = "puck_style"
    private const val KEY_SHAPE = "puck_shape"
    private const val KEY_CAR_COLOR = "puck_car_color"
}

/**
 * AutoMap 2026-10-27, Bluetooth-gated location puck (Robert, final 2026-10-27):
 * by DEFAULT the location shows a blue dot and the car appears only when a
 * Bluetooth device is connected; the "Car always on" toggle below overrides
 * that and always shows the car, even without Bluetooth. Navigation is NOT
 * gated — only the browse/location display uses this, so a route always
 * shows its puck. Bluetooth here means ANY connected Bluetooth device
 * (headset, A2DP audio, or GATT), not a car-specific profile: there is no
 * reliable car-only signal on stock Android, and guessing one would hide
 * the car from a paired car stereo that presents as plain A2DP.
 * [isBluetoothConnected] never throws: no permission, no adapter, or a
 * SecurityException all read as "not connected", so the blue dot stays
 * instead of crashing the map. BLUETOOTH_CONNECT (Android 12+) is a
 * runtime permission; the Settings page asks for it at the point of use.
 */
object CarAlwaysOn {
    /** True = always show the car, ignoring Bluetooth. False (default) = gated. */
    val on = mutableStateOf(false)

    fun init(context: Context) {
        on.value = prefs(context).getBoolean(KEY, false)
    }

    fun set(context: Context, value: Boolean) {
        on.value = value
        prefs(context).edit().putBoolean(KEY, value).apply()
    }

    fun isBluetoothConnected(context: Context): Boolean {
        return runCatching {
            if (Build.VERSION.SDK_INT >= 31 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
            ) return false
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return false
            val adapter: BluetoothAdapter = manager.adapter ?: return false
            if (!adapter.isEnabled) return false
            listOf(BluetoothProfile.HEADSET, BluetoothProfile.A2DP, BluetoothProfile.GATT).any {
                adapter.getProfileConnectionState(it) == BluetoothProfile.STATE_CONNECTED
            }
        }.getOrDefault(false)
    }

    private fun prefs(c: Context) = c.getSharedPreferences("vela_settings", Context.MODE_PRIVATE)
    private const val KEY = "car_always_on"
}

/** "Prefer buttons over swipes": keeps a discrete button wherever a gesture has one (today: the
 *  step-list button on the nav bar beside the swipe-up handle). Off by default; keypad-first
 *  devices behave as if it were on. */
/**
 * The picture-in-picture layout (user 2026-10-02). On (the default): the turn card across the top
 * with the time left along the bottom, which says more. Off: one bar along the bottom, Google's
 * mini map. The user tried Google's as the default the same day and went back.
 */
object PipTurnCard {
    val on = mutableStateOf(true)

    fun init(context: Context) {
        on.value = prefs(context).getBoolean(KEY, true)
    }

    fun set(context: Context, value: Boolean) {
        on.value = value
        prefs(context).edit().putBoolean(KEY, value).apply()
    }

    private fun prefs(c: Context) = c.getSharedPreferences("vela_settings", Context.MODE_PRIVATE)
    private const val KEY = "pip_turn_card"
}

object PreferButtons {
    val on = mutableStateOf(false)

    fun init(context: Context) {
        on.value = prefs(context).getBoolean(KEY, false)
    }

    fun set(context: Context, value: Boolean) {
        on.value = value
        prefs(context).edit().putBoolean(KEY, value).apply()
    }

    private fun prefs(c: Context) = c.getSharedPreferences("vela_settings", Context.MODE_PRIVATE)
    private const val KEY = "prefer_buttons"
}

/**
 * Where the PAUSE control sits during a drive (user 2026-09-18).
 *
 * On (the default) it takes the nav bar's right slot, which on a touch phone is an empty 54 dp
 * spacer holding the figures centered against End, and the right-edge stack keeps a plain mute
 * button. That puts the drive's two "hold something" controls where each is actually reached for:
 * pause beside End and the trip figures, mute up with the other map controls, neither of them
 * behind a pop-out.
 *
 * Off restores the combined button in the stack (tap opens, second tap pauses, long press mutes).
 *
 * The bar's right slot is also where the step-list button goes when it is asked for, and that
 * button wins: `PreferButtons` and a keypad-first device asked for a discrete target, so taking it
 * away to make room for this one is the wrong trade. The chevron handle opens the step list on its
 * own in both layouts, so nothing becomes unreachable either way.
 */
object PauseInBar {
    val on = mutableStateOf(true)

    fun init(context: Context) {
        on.value = prefs(context).getBoolean(KEY, true)
    }

    fun set(context: Context, value: Boolean) {
        on.value = value
        prefs(context).edit().putBoolean(KEY, value).apply()
    }

    private fun prefs(c: Context) = c.getSharedPreferences("vela_settings", Context.MODE_PRIVATE)
    private const val KEY = "nav_pause_in_bar"
}

/**
 * What happens to a faster-route offer nobody answers (issue #594).
 *
 * The offer used to sit there until it was answered, which is a prompt covering part of the map
 * asking a driver to make a decision with their hands on the wheel. It always resolves itself now.
 * On (the default, and what Google does) an unanswered offer is TAKEN: the route it names is
 * faster, that is the whole reason it appeared, and the drive continues either way. Off, it is
 * dismissed instead, for anyone who would rather keep the route they chose unless they say so.
 *
 * "Leave it on screen" is deliberately not one of the choices.
 */
object FasterRouteAuto {
    /** True = take it, false = let it go. */
    val accept = mutableStateOf(true)

    fun init(context: Context) {
        accept.value = prefs(context).getBoolean(KEY, true)
    }

    fun set(context: Context, value: Boolean) {
        accept.value = value
        prefs(context).edit().putBoolean(KEY, value).apply()
    }

    private fun prefs(c: Context) = c.getSharedPreferences("vela_settings", Context.MODE_PRIVATE)
    private const val KEY = "faster_route_auto"
}

/** Drives start north-up and flat instead of heading-up (issue #612, pref `nav_north_up`, off).
 *  The map's compass still toggles it during a drive. */
object NavNorthUp {
    val on = mutableStateOf(false)

    fun init(context: Context) {
        on.value = prefs(context).getBoolean(KEY, false)
    }

    fun set(context: Context, value: Boolean) {
        on.value = value
        prefs(context).edit().putBoolean(KEY, value).apply()
    }

    private fun prefs(c: Context) = c.getSharedPreferences("vela_settings", Context.MODE_PRIVATE)
    private const val KEY = "nav_north_up"
}
