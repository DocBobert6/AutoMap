package app.vela.ui.map

/**
 * The painted-road colours, in one place (audit pass 2, 2026-10-28).
 * Before this, the same four day/night values were written out as literals
 * in three places — the phone's PaintedRoadsLayer, the car's style JSON
 * (CarStyle.inject, day values only; the renderer re-colours for night),
 * and the car renderer's theme block — so a colour change had to land in
 * all three or the phone and car drifted. These are the values that were
 * already identical in all three; they are not new colours.
 *
 * Deliberately NOT here: the surface colour. The phone uses the basemap's
 * own road colour (or #AAB9C9), the car uses #a8b6c4 by day and #7a8fb5
 * by night — different values, so sharing it would change one of them.
 */
internal object PaintedPalette {
    fun median(dark: Boolean): String = if (dark) "#22303F" else "#E4E8EE"
    fun bike(dark: Boolean): String = if (dark) "#2E6B47" else "#86CFA0"
    fun center(dark: Boolean): String = if (dark) "#C9A227" else "#F2C200"
    fun white(dark: Boolean): String = if (dark) "#C9D2DC" else "#FFFFFF"
}
