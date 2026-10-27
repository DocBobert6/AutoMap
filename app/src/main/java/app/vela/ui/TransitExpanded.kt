package app.vela.ui

import androidx.compose.runtime.mutableStateMapOf
import app.vela.core.model.TransitItinerary

/**
 * Which transit itineraries in the directions panel are expanded (issue #812). A row's own
 * `remember` was lost when the panel dropped to its card, so an expanded route came back
 * collapsed while its line was still on the map. Keyed by what identifies an itinerary across
 * recompositions; [prune] keeps only the current results' keys.
 */
object TransitExpanded {
    val open = mutableStateMapOf<String, Boolean>()

    fun keyOf(t: TransitItinerary): String =
        "${t.departureEpochSec}|${t.arrivalEpochSec}|${t.steps.size}|${t.durationText}|${t.lines.joinToString(",") { it.name }}"

    fun prune(keep: Collection<String>) {
        val gone = open.keys.filter { it !in keep }
        gone.forEach { open.remove(it) }
    }
}
