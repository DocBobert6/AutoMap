package app.vela.ui

import android.content.Context
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** The locate and parking buttons' shape (issue #797): round, the default, or square-cornered
 *  for a home screen of Material You apps. Settings > Appearance > Map buttons, pref
 *  `map_button_shape`. */
object MapButtonShape {
    const val ROUND = "round"
    const val SQUARE = "square"

    val mode = mutableStateOf(ROUND)

    fun init(context: Context) {
        mode.value = prefs(context).getString(KEY, ROUND) ?: ROUND
    }

    fun set(context: Context, value: String) {
        mode.value = value
        prefs(context).edit().putString(KEY, value).apply()
    }

    /** The shape to draw the button in and to ring it with. */
    fun shape(): Shape = if (mode.value == SQUARE) RoundedCornerShape(16.dp) else CircleShape

    private fun prefs(c: Context) = c.getSharedPreferences("vela_settings", Context.MODE_PRIVATE)
    private const val KEY = "map_button_shape"
}
