package app.vela.core.model

/**
 * The zoom an area result is framed at, or null for a place. Google's keyless search answers a
 * country, a state, a county, a city or a postal code with no bounds (checked 2026-10-11, issue
 * #752), so the kind is read off what the entry carries: an area has no category and no rating,
 * a country or a state no address at all, a city or a county only its region as the address, and
 * a postal code the category "Postal code". The map used to fly to street level in the middle of
 * a country.
 */
fun Place.areaZoom(): Double? {
    // Only a search result can be an area. A saved place, a list entry or a dropped pin is
    // built with no category and no rating too, and one with no address read as a country.
    if (OWN_ID_PREFIXES.any { id.startsWith(it) }) return null
    if (rating != null || reviewCount != null) return null
    if (category.equals("Postal code", ignoreCase = true)) return POSTAL_ZOOM
    if (!category.isNullOrBlank()) return null
    val a = address?.trim().orEmpty()
    return when {
        a.isEmpty() -> COUNTRY_ZOOM
        a.none { it.isDigit() } && ',' !in a -> CITY_ZOOM
        else -> null
    }
}

/** The ids Vela makes itself: pins, addresses, imports, list and saved entries, contacts. */
private val OWN_ID_PREFIXES = listOf("pin:", "addr:", "geo:", "imp:", "import:", "list:", "saved:", "contact:")

const val COUNTRY_ZOOM = 6.5
const val CITY_ZOOM = 11.5
const val POSTAL_ZOOM = 13.0
