package app.vela.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import app.vela.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * AutoMap in-app updater, ported from AutoSpace's UpdateChecker pattern
 * (2026-10-27) and adapted to AutoMap. User-facing behaviour matches
 * AutoSpace: GitHub releases check, Stable/Beta/Alpha channels, at most
 * one automatic check a day, an Updates section in Settings with Check
 * for updates / Download / Install, the downloaded APK checked against
 * the size and SHA-256 GitHub lists for it, then handed to Android's
 * own installer (which enforces same package + same signing key).
 *
 * Divergences from AutoSpace, each deliberate and stated here:
 * - API base is DocBobert6/AutoMap, NOT upstream Vela. The former
 *   upstream updater pointed at PimpinPumpkin/Vela through VelaLinks and
 *   must NOT be repointed: VelaLinks also feeds map/place/road-feature
 *   downloads that AutoMap still takes from upstream. This updater has
 *   its own base constant and never touches VelaLinks.
 * - Channel default is ALPHA, not the version stage. AutoMap ships
 *   alphas, so a version-stage default would already be Alpha, but an
 *   explicit Alpha default survives a future stable version name.
 *   A saved channel always wins over the default.
 * - APK pick does NOT exclude "debug" names. AutoSpace excluded them
 *   because its debug builds are local dev builds; AutoMap's published
 *   alphas were debug-signed builds (AutoMap-*.apk) before alpha.7,
 *   so excluding them would then have made the updater find nothing
 *   at all. The pick is kept permissive for compatibility.
 * - No car-session / drive-ended / notification hooks. AutoSpace has a
 *   car session model; Vela does not have an equivalent, so those hooks
 *   are not forced in. The automatic check runs on app opened (see the
 *   MainActivity onResume hook) and the manual Check button always works.
 * - Debug gate kept (AutoSpace rule): automatic checks never run on
 *   debug builds. Published alphas are release-signed (since
 *   alpha.7), so the automatic check is live on them; the manual
 *   Check for updates button is NOT gated and works on any build.
 *   This is stated in the Settings copy, not hidden.
 * - A 404 from GitHub is NOT treated as "no stable release yet".
 *   AutoMap's repo is public (since 2026-10-27, alpha.7), but a 404
 *   can still mean the check could not reach the releases (e.g. the
 *   Stable channel's releases/latest, which excludes prereleases).
 *   Treating that as "up to date" would silently lie. A 404 therefore
 *   surfaces as a Failed state, so the limitation is visible instead
 *   of shipped as a false "up to date".
 * - No token, no browser-page fallback. Robert decided 2026-10-26
 *   explicitly against both: this updater is the AutoSpace pattern,
 *   nothing else. Since alpha.7, alphas are signed with one stable
 *   release key, so a signed alpha should update in-app over the
 *   previous signed alpha; only a debug-signed build still needs the
 *   old build uninstalled first.
 */
object AutoMapUpdater {

    data class Release(
        val version: String,
        val apkUrl: String,
        val size: Long,
        val sha256: String?,
        val pageUrl: String,
        // Release notes (GitHub release body), shown in Settings > About >
        // AutoMap updates BEFORE Download (2026-10-28, alpha.16). Null when
        // the release JSON carried no body, so the page shows the
        // "Release notes unavailable" fallback instead of invented notes.
        val notes: String? = null,
    )

    sealed interface State {
        data object Idle : State
        data object Checking : State
        data object UpToDate : State
        data class Available(val release: Release) : State
        data class Downloading(val release: Release, val fraction: Float) : State
        data class Ready(val release: Release, val apk: File) : State
        data class Failed(val message: String, val release: Release? = null) : State
    }

    private val flow = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = flow.asStateFlow()

    private const val PREFS = "automap_updates"
    private const val KEY_CHECKED_AT = "automap_checked_at"
    private const val KEY_FOUND = "automap_found"
    private const val KEY_AUTO = "automap_update_auto"
    private const val KEY_CHANNEL = "automap_update_channel"
    private const val DAY_MS = 24 * 60 * 60 * 1000L

    private const val LATEST = "https://api.github.com/repos/DocBobert6/AutoMap/releases/latest"
    private const val ALL = "https://api.github.com/repos/DocBobert6/AutoMap/releases?per_page=20"

    @Volatile private var installWhenAllowed = false

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun autoCheckEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_AUTO, true)

    fun setAutoCheckEnabled(context: Context, on: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO, on).apply()
    }

    enum class Channel { STABLE, BETA, ALPHA }

    fun channel(context: Context): Channel {
        val saved = prefs(context).getString(KEY_CHANNEL, null)
        return Channel.entries.firstOrNull { it.name == saved } ?: Channel.ALPHA
    }

    fun setChannel(context: Context, channel: Channel) {
        prefs(context).edit().putString(KEY_CHANNEL, channel.name)
            .remove(KEY_FOUND).remove(KEY_CHECKED_AT).apply()
        val current = flow.value
        if (current is State.Checking || current is State.Downloading) return
        // Audit pass 2 (L7): the old unconditional delete never checked its
        // result. A failed delete is now logged; the next check re-creates
        // the folder, so this stays a warning, not a new failure state.
        if (!updatesDir(context).deleteRecursively()) {
            android.util.Log.w("AutoMapUpdate", "update: could not clear the old update folder on channel change")
        }
        flow.value = State.Idle
        if (autoCheckEnabled(context) && !BuildConfig.DEBUG) check(context)
    }

    private fun dueForCheck(context: Context) =
        !BuildConfig.DEBUG && autoCheckEnabled(context) &&
            System.currentTimeMillis() - prefs(context).getLong(KEY_CHECKED_AT, 0) >= DAY_MS

    /** App came to the front: finish a pending install, restore a found release, auto-check if due. */
    fun onAppOpened(context: Context) {
        val current = flow.value
        if (installWhenAllowed && current is State.Ready &&
            context.packageManager.canRequestPackageInstalls()
        ) {
            installWhenAllowed = false
            install(context)
            return
        }
        if (current != State.Idle) return
        savedRelease(context)?.let { flow.value = State.Available(it) }
        if (dueForCheck(context)) check(context)
    }

    // Audit pass 2 (L2): the Checking/Downloading guard is now atomic —
    // the old plain flow.value reads let two near-simultaneous taps both
    // pass it. The guard + initial state set happen under one lock; the
    // thread body itself runs outside it. install() is untouched.
    private val guardLock = Any()

    private fun updateThread(body: () -> Unit): Thread =
        Thread(body, "automap-update").also { t ->
            t.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, e ->
                android.util.Log.w("AutoMapUpdate", "update: uncaught on update thread: ${e.message}")
                flow.value = State.Failed(e.message ?: "The update failed. Try again.")
            }
        }

    fun check(context: Context) {
        val app = context.applicationContext
        synchronized(guardLock) {
            if (flow.value is State.Checking || flow.value is State.Downloading) return
            flow.value = State.Checking
        }
        updateThread({ flow.value = checkNow(app) }).start()
    }

    private fun checkNow(app: Context): State = runCatching { fetchNewest(channel(app)) }.fold(
        onSuccess = { release ->
            prefs(app).edit().putLong(KEY_CHECKED_AT, System.currentTimeMillis()).apply()
            if (release != null && isNewer(release.version, BuildConfig.VERSION_NAME)) {
                android.util.Log.i("AutoMapUpdate", "update: ${release.version} is out (this is ${BuildConfig.VERSION_NAME})")
                save(app, release)
                State.Available(release)
            } else {
                prefs(app).edit().remove(KEY_FOUND).apply()
                updatesDir(app).deleteRecursively()
                State.UpToDate
            }
        },
        onFailure = {
            android.util.Log.w("AutoMapUpdate", "update: could not check: ${it.message}")
            State.Failed(it.message ?: "The update check failed. Try again.")
        },
    )

    fun download(context: Context, release: Release) {
        val app = context.applicationContext
        synchronized(guardLock) {
            if (flow.value is State.Downloading) return
            flow.value = State.Downloading(release, 0f)
        }
        updateThread({
            // Audit pass 2 (L7): the folder create used to run outside the
            // runCatching and its mkdirs() result was never checked, so
            // storage pressure left no cached APK and no error until the
            // fetch failed. It is now inside, and a failed create is a
            // plain Failed state instead of a silent dead end.
            val dir = updatesDir(app)
            val apk = File(dir, "AutoMap-${release.version}.apk")
            flow.value = runCatching {
                dir.deleteRecursively()
                if (!dir.exists() && !dir.mkdirs()) error("Could not create the update folder. Try again.")
                fetchApk(release, apk)
                android.util.Log.i("AutoMapUpdate", "update: downloaded ${release.version}")
                State.Ready(release, apk)
            }.getOrElse {
                apk.delete()
                android.util.Log.w("AutoMapUpdate", "update: download failed: ${it.message}")
                State.Failed(it.message ?: "The download failed. Try again.", release)
            }
            if (flow.value is State.Ready) install(app)
        }).start()
    }

    /** Hands the downloaded APK to Android's installer, via the unknown-sources flow if needed.
     *  2026-10-28 crash fix: this used to sit outside any try/catch, so a
     *  FileProvider/install failure (e.g. a missing provider root) killed
     *  the app with no dialog. Now any failure lands in a Failed state
     *  with a message instead. All install entry points (download thread,
     *  onAppOpened, Ready-UI Install button) funnel through here. */
    fun install(context: Context) {
        val ready = flow.value as? State.Ready ?: return
        try {
            val pm = context.packageManager
            if (!pm.canRequestPackageInstalls()) {
                installWhenAllowed = true
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}"),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                return
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", ready.apk)
            context.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: Exception) {
            android.util.Log.w("AutoMapUpdate", "update: install failed: ${e.message}")
            flow.value = State.Failed(
                "Could not open the installer: ${e.message ?: "unknown error"}. Try downloading again.",
                ready.release,
            )
        }
    }

    /** The newest release with an APK that [channel] takes. Drafts never count. */
    private fun fetchNewest(channel: Channel): Release? {
        val stable = channel == Channel.STABLE
        val conn = (URL(if (stable) LATEST else ALL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "AutoMap/${BuildConfig.VERSION_NAME}")
        }
        try {
            if (conn.responseCode == 404) {
                // A 404 is indistinguishable from "no release" over an
                // unauthenticated request, so this must surface as a
                // failure, not as "up to date".
                error("GitHub answered 404, so the update check could not read the releases. Try again.")
            }
            if (conn.responseCode !in 200..299) error("GitHub answered ${conn.responseCode}")
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            if (stable) return release(JSONObject(body))
            val list = JSONArray(body)
            return (0 until list.length()).map { list.getJSONObject(it) }
                .filterNot { it.optBoolean("draft") }
                .filter {
                    channel == Channel.ALPHA || !it.optBoolean("prerelease") ||
                        it.optString("tag_name").contains("beta", ignoreCase = true)
                }
                .mapNotNull(::release)
                .reduceOrNull { best, next -> if (isNewer(next.version, best.version)) next else best }
        } finally {
            conn.disconnect()
        }
    }

    /** A release as GitHub lists it, or null when it has no APK attached. */
    private fun release(json: JSONObject): Release? {
        val assets = json.optJSONArray("assets") ?: return null
        val all = (0 until assets.length()).map { assets.getJSONObject(it) }
        val apk = all.firstOrNull {
            it.optString("name").startsWith("AutoMap-") && it.optString("name").endsWith(".apk")
        } ?: all.firstOrNull { it.optString("name").endsWith(".apk") } ?: return null
        return Release(
            version = json.getString("tag_name").removePrefix("v"),
            apkUrl = apk.getString("browser_download_url"),
            size = apk.optLong("size", -1),
            sha256 = apk.optString("digest").takeIf { it.startsWith("sha256:") }?.removePrefix("sha256:"),
            pageUrl = json.optString("html_url"),
            notes = json.optString("body").takeIf { it.isNotBlank() },
        )
    }

    private fun fetchApk(release: Release, into: File) {
        val conn = (URL(release.apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("User-Agent", "AutoMap/${BuildConfig.VERSION_NAME}")
        }
        try {
            if (conn.responseCode !in 200..299) error("GitHub answered ${conn.responseCode}")
            val total = release.size.takeIf { it > 0 } ?: conn.contentLengthLong
            val digest = MessageDigest.getInstance("SHA-256")
            var done = 0L
            var shown = -1
            conn.inputStream.use { input ->
                into.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        digest.update(buffer, 0, n)
                        done += n
                        val percent = if (total > 0) (done * 100 / total).toInt() else 0
                        if (percent != shown) {
                            shown = percent
                            flow.value = State.Downloading(release, percent / 100f)
                        }
                    }
                }
            }
            if (release.size > 0 && done != release.size) error("The download stopped early. Try again.")
            val hex = digest.digest().joinToString("") { "%02x".format(it) }
            if (release.sha256 != null && !hex.equals(release.sha256, ignoreCase = true)) {
                error("The download came out damaged. Try again.")
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun updatesDir(context: Context) = File(context.cacheDir, "updates")

    private fun save(context: Context, release: Release) {
        val json = JSONObject()
            .put("version", release.version)
            .put("apkUrl", release.apkUrl)
            .put("size", release.size)
            .put("sha256", release.sha256 ?: "")
            .put("pageUrl", release.pageUrl)
            .put("notes", release.notes ?: "")
        prefs(context).edit().putString(KEY_FOUND, json.toString()).apply()
    }

    private fun savedRelease(context: Context): Release? {
        val json = prefs(context).getString(KEY_FOUND, null)?.let { runCatching { JSONObject(it) }.getOrNull() } ?: return null
        val release = Release(
            version = json.optString("version"),
            apkUrl = json.optString("apkUrl"),
            size = json.optLong("size", -1),
            sha256 = json.optString("sha256").ifBlank { null },
            pageUrl = json.optString("pageUrl"),
            notes = json.optString("notes").ifBlank { null },
        )
        return release.takeIf { isNewer(it.version, BuildConfig.VERSION_NAME) }
    }

    /**
     * Version comparison for AutoMap tags. AutoSpace's version of this
     * only compared the numbers before the dash and then the stage
     * (stable > beta > alpha), which cannot tell 0.3.0-alpha.2 from
     * 0.3.0-alpha.1 at all, so it is not reused here. This version
     * compares the numeric base first, then the prerelease tokens in
     * order (alpha < beta < stable, numeric suffixes compared as
     * numbers, so alpha.2 beats alpha.1). One deliberate exception:
     * a prerelease of 0.3.0 beats plain 0.3.0, because 0.3.0 was never
     * a stable release — it was the versionName of every pre-alpha
     * debug build (versionCode 1), and the alpha scheme (starting at
     * 0.3.0-alpha.1 / code 2) is what those builds migrate to. For any
     * other base version, a plain release still beats its own
     * prereleases, as usual. Pure, so it can be pinned by a unit test
     * without a device.
     */
    internal fun isNewer(candidate: String, installed: String): Boolean {
        val c = candidate.removePrefix("v")
        val i = installed.removePrefix("v")
        if (c == i) return false
        fun base(v: String) = v.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        fun pre(v: String) = v.substringAfter('-', "").takeIf { it.isNotEmpty() }?.split('.').orEmpty()
        val cb = base(c); val ib = base(i)
        for (k in 0 until maxOf(cb.size, ib.size)) {
            val x = cb.getOrElse(k) { 0 }; val y = ib.getOrElse(k) { 0 }
            if (x != y) return x > y
        }
        // Same numeric base.
        val cp = pre(c); val ip = pre(i)
        if (cp.isEmpty() && ip.isEmpty()) return false
        if (cp.isEmpty()) return true // stable beats any prerelease of the same base
        if (ip.isEmpty()) return i == "0.3.0" // legacy pre-alpha debug version only
        fun rank(t: String) = when (t.lowercase()) {
            "alpha" -> 1
            "beta" -> 2
            else -> 0
        }
        for (k in 0 until maxOf(cp.size, ip.size)) {
            val x = cp.getOrElse(k) { "" }
            val y = ip.getOrElse(k) { "" }
            if (x == y) continue
            val xn = x.toIntOrNull(); val yn = y.toIntOrNull()
            if (xn != null && yn != null) return xn > yn
            if (xn != null) return false // a number after a word is a suffix, not a stage
            if (yn != null) return true
            val rx = rank(x); val ry = rank(y)
            if (rx != ry) return rx > ry
            return x > y
        }
        return false
    }
}
