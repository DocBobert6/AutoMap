package app.vela.ui.settings.sections

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.vela.R
import app.vela.ui.PuckStyle
import app.vela.ui.map.PuckModels
import app.vela.ui.map.altimaTopDownPreviewBitmap
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * AutoMap vehicle colour picker (2026-10-26): the top-down 3D Altima preview, drawn by the
 * same mesh code as the map puck, plus a colour wheel, HSV and RGB sliders, and a hex field,
 * all kept in sync and all writing the same persisted preference. An invalid hex entry is
 * rejected (shown as an error, never applied); a valid one applies immediately.
 */
@Composable
internal fun AltimaColorPicker() {
    val context = LocalContext.current
    val currentHex = PuckStyle.carColor.value
    val bodyInt = runCatching { android.graphics.Color.parseColor(currentHex) }.getOrDefault(0xFFD93025.toInt())
    val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(bodyInt, it) }
    val hue = hsv[0]; val sat = hsv[1]; val value = hsv[2]
    val red = android.graphics.Color.red(bodyInt)
    val green = android.graphics.Color.green(bodyInt)
    val blue = android.graphics.Color.blue(bodyInt)

    fun commitHex(hex: String) = PuckStyle.setCarColor(context, hex)
    fun commitHsv(h: Float, s: Float, v: Float) {
        commitHex(String.format(java.util.Locale.US, "#%06X", 0xFFFFFF and android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))))
    }
    fun commitRgb(r: Int, g: Int, b: Int) {
        commitHex(String.format(java.util.Locale.US, "#%06X", 0xFFFFFF and android.graphics.Color.rgb(r, g, b)))
    }

    Text(
        stringResource(R.string.settings_puck_color_title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp),
    )
    // The large preview: the exact Altima mesh, top-down 3D, in the picked colour.
    val mesh = PuckModels.forShape(PuckStyle.SHAPE_CAR, currentHex)
    if (mesh != null) {
        val preview = remember(currentHex) { altimaTopDownPreviewBitmap(mesh, 360).asImageBitmap() }
        Image(preview, contentDescription = null, modifier = Modifier.padding(start = 20.dp, top = 4.dp).size(144.dp))
    }
    Text(
        stringResource(R.string.settings_puck_color_hint),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
    )

    // Colour wheel: hue around the circle, saturation from centre out, value from the V slider.
    val wheelPx = 240
    val wheelBitmap = remember(value) {
        val bmp = Bitmap.createBitmap(wheelPx, wheelPx, Bitmap.Config.ARGB_8888)
        val cx = wheelPx / 2f; val cy = wheelPx / 2f; val rad = wheelPx / 2f
        for (y in 0 until wheelPx) {
            for (x in 0 until wheelPx) {
                val dx = x - cx; val dy = y - cy
                val dist = sqrt(dx * dx + dy * dy)
                if (dist <= rad) {
                    val h = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 360.0) % 360.0).toFloat()
                    bmp.setPixel(x, y, android.graphics.Color.HSVToColor(floatArrayOf(h, (dist / rad).coerceIn(0f, 1f), value)))
                }
            }
        }
        bmp
    }
    val wheelImage = remember(wheelBitmap) { wheelBitmap.asImageBitmap() }
    fun wheelPick(at: Offset, sizePx: Float) {
        val cx = sizePx / 2f; val cy = sizePx / 2f; val rad = sizePx / 2f
        val dx = at.x - cx; val dy = at.y - cy
        val dist = sqrt(dx * dx + dy * dy)
        if (dist > rad) return
        val h = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 360.0) % 360.0).toFloat()
        commitHsv(h, (dist / rad).coerceIn(0f, 1f), value)
    }
    Canvas(
        modifier = Modifier.padding(start = 20.dp, top = 4.dp).size(220.dp)
            .pointerInput(value) {
                detectTapGestures { at -> wheelPick(at, size.width.toFloat()) }
            }
            .pointerInput(value) {
                detectDragGestures { change, _ -> wheelPick(change.position, size.width.toFloat()) }
            },
    ) {
        drawImage(wheelImage, dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()))
        // Knob at the current hue/saturation.
        val angle = Math.toRadians(hue.toDouble())
        val kx = size.width / 2f + (sat * size.width / 2f * 0.96f * kotlin.math.cos(angle)).toFloat()
        val ky = size.height / 2f + (sat * size.height / 2f * 0.96f * kotlin.math.sin(angle)).toFloat()
        drawCircle(Color.White, radius = 12f, center = Offset(kx, ky))
        drawCircle(Color(bodyInt), radius = 9f, center = Offset(kx, ky))
    }

    // Hex field: with or without a leading hash; invalid input is an error, never applied.
    var hexText by remember(currentHex) { mutableStateOf(currentHex) }
    val hexValid = Regex("^#?[0-9A-Fa-f]{6}$").matches(hexText)
    val hexShowError = hexText.isNotBlank() && !hexValid &&
        (hexText.length >= 6 || hexText.any { it !in "#0123456789abcdefABCDEF" })
    TextField(
        value = hexText,
        onValueChange = { text ->
            hexText = text
            if (Regex("^#?[0-9A-Fa-f]{6}$").matches(text)) {
                commitHex("#" + text.trimStart('#').uppercase(java.util.Locale.US))
            }
        },
        label = { Text(stringResource(R.string.settings_puck_hex_label)) },
        isError = hexShowError,
        supportingText = if (hexShowError) {
            { Text(stringResource(R.string.settings_puck_hex_invalid)) }
        } else null,
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
    )

    // Sliders: HSV first (the wheel's own space), then RGB for exact channel work.
    @Composable
    fun SliderRow(label: String, sliderValue: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit, shown: String) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp)) {
            Row {
                Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(shown, style = MaterialTheme.typography.bodyMedium)
            }
            Slider(value = sliderValue, onValueChange = onChange, valueRange = range)
        }
    }
    SliderRow(stringResource(R.string.settings_puck_hue), hue, 0f..360f, { commitHsv(it, sat, value) }, hue.toInt().toString())
    SliderRow(stringResource(R.string.settings_puck_saturation), sat, 0f..1f, { commitHsv(hue, it, value) }, "${(sat * 100).toInt()}%")
    SliderRow(stringResource(R.string.settings_puck_brightness), value, 0f..1f, { commitHsv(hue, sat, it) }, "${(value * 100).toInt()}%")
    SliderRow(stringResource(R.string.settings_puck_red), red.toFloat(), 0f..255f, { commitRgb(it.toInt(), green, blue) }, red.toString())
    SliderRow(stringResource(R.string.settings_puck_green), green.toFloat(), 0f..255f, { commitRgb(red, it.toInt(), blue) }, green.toString())
    SliderRow(stringResource(R.string.settings_puck_blue), blue.toFloat(), 0f..255f, { commitRgb(red, green, it.toInt()) }, blue.toString())

    Row(Modifier.padding(start = 20.dp, top = 4.dp, bottom = 8.dp)) {
        TextButton(onClick = { commitHex("#D93025") }) {
            Text(stringResource(R.string.settings_puck_reset))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            currentHex,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
