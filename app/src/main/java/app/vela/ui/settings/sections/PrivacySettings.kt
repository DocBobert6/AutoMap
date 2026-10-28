package app.vela.ui.settings.sections

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.layout.fillMaxWidth
import app.vela.ui.settings.settingsAnchor
import app.vela.ui.dpadHighlight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.vela.R
import app.vela.ui.settings.PageIntro
import app.vela.ui.settings.SettingsGroup
import app.vela.ui.settings.SettingsScaffold
import app.vela.ui.dpadHighlight // D-pad-only operation (docs/dpad.md)

/** Privacy sub-screen (2026-10-28 restructure): a sub-hub listing Google-free & data,
 *  What Google is used for (See also LINKS to the owning pages, not a second copy of their
 *  rows — Robert's decision 3), and History. The old "what Google is used for" list and the
 *  Google Maps links row moved to Navigation > Links from other apps. */
@Composable
internal fun PrivacySettingsScreen(vm: app.vela.ui.map.MapViewModel, onBack: () -> Unit, onOpenSection: (app.vela.ui.settings.SettingsSection) -> Unit = {}) {
    val context = LocalContext.current
    var page by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    if (page == null) {
        SettingsScaffold(stringResource(R.string.settings_privacy), onBack) { _ ->
            Spacer(Modifier.height(4.dp))
            app.vela.ui.settings.CategoryRow(stringResource(R.string.settings_cat_google_free), stringResource(R.string.settings_cat_google_free_sub)) { page = "googlefree" }
            app.vela.ui.settings.CategoryRow(stringResource(R.string.settings_cat_google_uses), stringResource(R.string.settings_cat_google_uses_sub)) { page = "uses" }
            app.vela.ui.settings.CategoryRow(stringResource(R.string.settings_cat_history), stringResource(R.string.settings_cat_history_sub)) { page = "history" }
            Spacer(Modifier.height(24.dp))
        }
        return
    }
    val pageTitle = when (page) {
        "googlefree" -> stringResource(R.string.settings_cat_google_free)
        "uses" -> stringResource(R.string.settings_cat_google_uses)
        else -> stringResource(R.string.settings_cat_history)
    }
    SettingsScaffold(pageTitle, { page = null }) { topRow ->
        Spacer(Modifier.height(4.dp))
        if (page == "googlefree") {
        PageIntro(stringResource(R.string.settings_data_privacy_hint))
        SettingsGroup {
            app.vela.ui.settings.ToggleRow(
                label = stringResource(R.string.settings_google_free),
                checked = app.vela.ui.GoogleFree.on.value,
                onCheckedChange = { app.vela.ui.GoogleFree.set(context, it) },
                hint = stringResource(R.string.settings_google_free_hint),
                switchModifier = topRow,
            )
            if (app.vela.ui.GoogleFree.on.value) {
                app.vela.ui.settings.GroupDivider()
                app.vela.ui.settings.ToggleRow(
                    label = stringResource(R.string.settings_google_free_links),
                    checked = app.vela.ui.GoogleFree.resolveLinks.value,
                    onCheckedChange = { app.vela.ui.GoogleFree.setResolveLinks(context, it) },
                    hint = stringResource(R.string.settings_google_free_links_hint),
                )
            }
        }
        if (!app.vela.ui.GoogleFree.on.value) {
            Spacer(Modifier.height(8.dp))
            SettingsGroup {
                app.vela.ui.settings.SubHead(stringResource(R.string.settings_google_session))
                app.vela.ui.settings.Hint(stringResource(R.string.settings_google_session_hint))
                if (app.vela.web.GoogleStanding.limited.value) {
                    app.vela.ui.settings.Hint(stringResource(R.string.settings_google_session_limited))
                }
                val rot = app.vela.web.SessionRotation
                listOf(
                    rot.WEEK to R.string.settings_google_session_week,
                    rot.DAY to R.string.settings_google_session_day,
                    rot.LAUNCH to R.string.settings_google_session_launch,
                ).forEach { (key, label) ->
                    app.vela.ui.settings.SelectableRow(
                        label = stringResource(label),
                        selected = rot.mode.value == key,
                        onClick = { rot.setMode(context, key) },
                    )
                }
                if (rot.mode.value == rot.LAUNCH) {
                    app.vela.ui.settings.Hint(stringResource(R.string.settings_google_session_launch_hint))
                }
                androidx.compose.foundation.layout.Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    FilledTonalButton(
                        modifier = Modifier.dpadHighlight(androidx.compose.material3.ButtonDefaults.filledTonalShape),
                        onClick = {
                            rot.startNewNow(context)
                            android.widget.Toast.makeText(context, context.getString(R.string.settings_google_session_done), android.widget.Toast.LENGTH_SHORT).show()
                        },
                    ) { Text(stringResource(R.string.settings_google_session_now)) }
                }
                app.vela.ui.settings.ToggleRow(
                    label = stringResource(R.string.settings_block_google_telemetry),
                    checked = app.vela.web.GoogleTelemetry.block.value,
                    onCheckedChange = { app.vela.web.GoogleTelemetry.set(context, it) },
                    hint = stringResource(R.string.settings_block_google_telemetry_hint),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        GoogleUsageGroup()
        Spacer(Modifier.height(8.dp))
        SettingsGroup {
        androidx.compose.foundation.layout.Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        FilledTonalButton(
            modifier = Modifier.dpadHighlight(androidx.compose.material3.ButtonDefaults.filledTonalShape),
            onClick = {
                runCatching {
                    context.startActivity(
                        android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(app.vela.core.config.VelaLinks.privacy()),
                        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            },
        ) { Text(stringResource(R.string.settings_privacy_button)) }
        }
        }
        } // googlefree
        if (page == "uses") {
        // See also links only: each row opens the page that OWNS the setting, so there is
        // exactly one copy of every switch.
        PageIntro(stringResource(R.string.settings_cat_google_uses_sub))
        app.vela.ui.settings.CategoryRow(stringResource(R.string.settings_places_source), stringResource(R.string.settings_cat_places_map_sub)) { onOpenSection(app.vela.ui.settings.SettingsSection.PLACES) }
        app.vela.ui.settings.CategoryRow(stringResource(R.string.settings_place_pages), stringResource(R.string.settings_cat_place_pages_sub)) { onOpenSection(app.vela.ui.settings.SettingsSection.PLACES) }
        app.vela.ui.settings.CategoryRow(stringResource(R.string.settings_transit_layer), stringResource(R.string.settings_cat_layers_sub)) { onOpenSection(app.vela.ui.settings.SettingsSection.MAP) }
        app.vela.ui.settings.CategoryRow(stringResource(R.string.settings_full_place_load), stringResource(R.string.settings_cat_place_pages_sub)) { onOpenSection(app.vela.ui.settings.SettingsSection.PLACES) }
        app.vela.ui.settings.CategoryRow(stringResource(R.string.settings_other_locations_auto), stringResource(R.string.settings_cat_search_behaviour_sub)) { onOpenSection(app.vela.ui.settings.SettingsSection.SEARCH) }
        app.vela.ui.settings.CategoryRow(stringResource(R.string.settings_route_traffic_on_tap), stringResource(R.string.settings_cat_route_options_sub)) { onOpenSection(app.vela.ui.settings.SettingsSection.NAVIGATION) }
        app.vela.ui.settings.CategoryRow(stringResource(R.string.settings_live_rechecks), stringResource(R.string.settings_cat_route_options_sub)) { onOpenSection(app.vela.ui.settings.SettingsSection.NAVIGATION) }
        } // uses
        if (page == "history") {
        var confirmClear by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
        SettingsGroup {
        androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth().settingsAnchor(stringResource(R.string.settings_clear_history)).padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(stringResource(R.string.settings_clear_history), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.settings_clear_history_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            FilledTonalButton(
                modifier = Modifier.padding(top = 8.dp).dpadHighlight(androidx.compose.material3.ButtonDefaults.filledTonalShape),
                onClick = { confirmClear = true },
            ) { Text(stringResource(R.string.settings_clear_history_action)) }
        }
        }
        if (confirmClear) {
            app.vela.ui.VelaDialog(
                onDismissRequest = { confirmClear = false },
                title = stringResource(R.string.settings_clear_history_confirm_title),
                confirmText = stringResource(R.string.settings_clear_history_action),
                onConfirm = {
                    vm.clearAllHistory()
                    confirmClear = false
                    android.widget.Toast.makeText(context, context.getString(R.string.settings_clear_history_done), android.widget.Toast.LENGTH_SHORT).show()
                },
                dismissText = stringResource(android.R.string.cancel),
                onDismiss = { confirmClear = false },
            ) {
                Text(stringResource(R.string.settings_clear_history_confirm_body), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(8.dp))
        app.vela.ui.settings.CategoryRow(stringResource(R.string.settings_parking_history), stringResource(R.string.settings_cat_saved_routes_sub)) { onOpenSection(app.vela.ui.settings.SettingsSection.SAVED_PLACES) }
        app.vela.ui.settings.CategoryRow(stringResource(R.string.settings_cat_trips), stringResource(R.string.settings_cat_trips_sub)) { onOpenSection(app.vela.ui.settings.SettingsSection.DIAGNOSTICS) }
        } // history
        Spacer(Modifier.height(24.dp))
    }
}

/** How many requests went to Google, today and over the last week, by purpose (2026-09-25,
 *  core/net/GoogleUsage). Read when the page opens; the numbers are a snapshot, not live. */
@Composable
private fun GoogleUsageGroup() {
    val today = androidx.compose.runtime.remember { app.vela.diag.GoogleUsageStore.today() }
    val week = androidx.compose.runtime.remember { app.vela.diag.GoogleUsageStore.week() }
    val avg = androidx.compose.runtime.remember { app.vela.diag.GoogleUsageStore.weekDailyAverage() }
    SettingsGroup {
        app.vela.ui.settings.SubHead(stringResource(R.string.settings_google_usage))
        app.vela.ui.settings.Hint(stringResource(R.string.settings_google_usage_hint))
        if (week.isEmpty()) {
            app.vela.ui.settings.Hint(stringResource(R.string.settings_google_usage_none))
        } else {
            Text(
                stringResource(R.string.settings_google_usage_summary, today.sumOf { it.second }, avg),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            val todayMap = today.toMap()
            week.forEach { (kind, n) ->
                Text(
                    stringResource(R.string.settings_google_usage_row, googleUsageLabel(kind), todayMap[kind] ?: 0, n),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun googleUsageLabel(kind: String): String = when {
    kind == "search" -> stringResource(R.string.google_usage_kind_search)
    kind == "nearby places" -> stringResource(R.string.google_usage_kind_nearby)
    kind == "place details" -> stringResource(R.string.google_usage_kind_details)
    kind == "photos" -> stringResource(R.string.google_usage_kind_photos)
    kind == "reviews" -> stringResource(R.string.google_usage_kind_reviews)
    kind == "images" -> stringResource(R.string.google_usage_kind_images)
    kind == "directions" -> stringResource(R.string.google_usage_kind_directions)
    kind == "suggestions" -> stringResource(R.string.google_usage_kind_suggestions)
    kind == "street view" -> stringResource(R.string.google_usage_kind_streetview)
    kind == "session" -> stringResource(R.string.google_usage_kind_session)
    kind == "shared list" -> stringResource(R.string.google_usage_kind_lists)
    kind == "page resources" -> stringResource(R.string.google_usage_kind_page_resources)
    kind.startsWith("page: ") -> stringResource(R.string.google_usage_kind_page, kind.removePrefix("page: "))
    else -> stringResource(R.string.google_usage_kind_other)
}

