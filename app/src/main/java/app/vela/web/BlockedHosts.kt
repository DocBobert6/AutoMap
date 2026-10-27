package app.vela.web

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

/**
 * Hosts and paths that are telemetry, ads or logging on Google's pages, not anything a scrape
 * or the reviews panel needs: fed an empty response so the hidden pages load less and the phone
 * calls fewer third parties (a tracker blocker on /e/OS counted googlesyndication every minute of
 * use, issue #751). The maps pages themselves need google.com, gstatic.com (assets) and
 * googleusercontent.com (photos), which pass.
 */
internal object BlockedHosts {
    private val HOSTS = listOf(
        "doubleclick.net", "googleadservices.com", "googlesyndication.com",
        "google-analytics.com", "googletagmanager.com", "adservice.google",
        "csi.gstatic.com",
    )
    private val PATHS = listOf("/gen_204", "/log204", "/log?", "/domainreliability/")

    fun blocked(req: WebResourceRequest): Boolean {
        val url = req.url ?: return false
        val host = url.host.orEmpty()
        val path = url.path.orEmpty() + "?" + url.query.orEmpty()
        if (HOSTS.any { host == it || host.endsWith(".$it") || host.contains(it) }) return true
        if (PATHS.any { path.contains(it) }) return true
        // play.google.com/log: Chrome and Maps client telemetry uploads.
        if (host == "play.google.com" && path.startsWith("/log")) return true
        return false
    }

    /** The empty answer a blocked request gets. */
    fun empty(): WebResourceResponse = WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
}
