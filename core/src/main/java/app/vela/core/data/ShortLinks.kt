package app.vela.core.data

import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder

/**
 * Short Google Maps share links (`maps.app.goo.gl/...`, `goo.gl/maps/...`) carry nothing but a
 * code; only Google's link shortener knows where one points. [resolve] asks it ONCE per hop with no
 * cookies, reads the redirect's `Location` and stops before loading any Google page, so what Google
 * sees is one request to its shortener. The target is then read on the phone by [MapLinkParser].
 *
 * Used with "Use Vela without Google" on only when the user leaves "Open shared Google Maps links"
 * on (the default): the alternative is not being able to view a link someone sent at all.
 */
object ShortLinks {
    private val SHORTENER_HOSTS = setOf("maps.app.goo.gl", "goo.gl", "g.co")

    fun isShortener(url: String): Boolean = hostOf(url) in SHORTENER_HOSTS

    /** A shared LIST; its places live on Google's servers. */
    fun isList(url: String): Boolean = "/maps/placelists" in url || "/entitylist/" in url || listId(url) != null

    /** The list's id from a share link's target: the older `/maps/placelists/list/<id>` page, or
     *  the form the shortener answers with since 2026-10, `/maps/@/data=...!11m2!2s<id>!3e3`,
     *  where the id sits in the data blob and the page carries nothing else. */
    fun listId(url: String): String? {
        val u = runCatching { URLDecoder.decode(url, "UTF-8") }.getOrDefault(url)
        LIST_PATH.find(u)?.let { return it.groupValues[1] }
        return LIST_DATA.find(u)?.groupValues?.get(1)
    }

    private val LIST_PATH = Regex("""/maps/placelists/list/([A-Za-z0-9_-]+)""")
    private val LIST_DATA = Regex("""!11m2!2s([A-Za-z0-9_-]+)!3e""")

    /** The next URL to read from a redirect: an EU consent interstitial carries the real target
     *  in its `continue` parameter, which is unwrapped here rather than requested. */
    fun nextHop(location: String): String {
        if (hostOf(location) == "consent.google.com") {
            val cont = location.substringAfter('?', "").split('&')
                .firstOrNull { it.startsWith("continue=") }?.substringAfter('=')
            if (!cont.isNullOrBlank()) return runCatching { URLDecoder.decode(cont, "UTF-8") }.getOrDefault(cont)
        }
        return location
    }

    /** Follows shortener hops only (at most [maxHops]) and returns the first non-shortener target,
     *  or null when the shortener did not answer with a redirect. Blocking; call off the main thread. */
    fun resolve(http: OkHttpClient, url: String, userAgent: String, maxHops: Int = 3): String? {
        val client = http.newBuilder()
            .followRedirects(false).followSslRedirects(false)
            .cookieJar(okhttp3.CookieJar.NO_COOKIES)
            .build()
        var current = url.trim().let { if (it.startsWith("http")) it else "https://$it" }
        repeat(maxHops) {
            val req = Request.Builder().url(current).header("User-Agent", userAgent).build()
            val location = client.newCall(req).execute().use { resp ->
                if (!resp.isRedirect) return null
                resp.header("Location")
            } ?: return null
            val next = nextHop(location)
            if (!isShortener(next)) return next
            current = next
        }
        return null
    }

    private fun hostOf(url: String): String =
        url.substringAfter("://", url).substringBefore('/').substringBefore('?').substringBefore(':').lowercase()
}
