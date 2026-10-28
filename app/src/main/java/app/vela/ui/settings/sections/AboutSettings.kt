package app.vela.ui.settings.sections

import app.vela.ui.icons.Sym

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vela.BuildConfig
import app.vela.R
import app.vela.ui.DpadRingBox // D-pad-only operation (docs/dpad.md)
import app.vela.ui.Onboarding
import app.vela.ui.map.MapViewModel
import app.vela.ui.settings.GroupDivider
import app.vela.ui.settings.Hint
import app.vela.ui.settings.PageIntro
import app.vela.ui.settings.SelectableRow
import app.vela.ui.settings.CategoryRow
import app.vela.ui.settings.SettingsGroup
import app.vela.ui.settings.SettingsScaffold
import app.vela.ui.settings.ToggleRow
import app.vela.ui.dpadHighlight
import app.vela.ui.dpadRowSibling

/** About sub-screen: the project blurb, version + the in-app updater. */
@Composable
internal fun AboutSettingsScreen(vm: MapViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var page by remember { mutableStateOf<String?>(null) }
    if (page == null) {
        SettingsScaffold(stringResource(R.string.settings_about), onBack) { _ ->
            Spacer(Modifier.height(4.dp))
            CategoryRow(stringResource(R.string.settings_cat_about), stringResource(R.string.settings_cat_about_sub)) { page = "about" }
            CategoryRow(stringResource(R.string.settings_cat_updates), stringResource(R.string.settings_cat_updates_sub)) { page = "updates" }
            Spacer(Modifier.height(24.dp))
        }
        return
    }
    val pageTitle = if (page == "about") stringResource(R.string.settings_cat_about) else stringResource(R.string.settings_cat_updates)
    SettingsScaffold(pageTitle, { page = null }) { topRow ->
        Spacer(Modifier.height(4.dp))
        PageIntro(stringResource(R.string.settings_about_hint))
        if (page == "about") {
        Spacer(Modifier.height(8.dp))
        Spacer(Modifier.height(8.dp))
        // The project itself (issue #739): people go back and forth between the app and GitHub.
        SettingsGroup(title = stringResource(R.string.settings_project)) {
        androidx.compose.foundation.layout.Column(Modifier.padding(horizontal = 16.dp)) {
        Hint(stringResource(R.string.settings_project_hint))
        Hint(stringResource(R.string.settings_project_credit))
        Spacer(Modifier.height(4.dp))
        // Two doors, each its own link, so either host can move without the other going dark.
        val projFocus = remember { List(2) { FocusRequester() } }
        fun open(url: String) = runCatching {
            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
        }
        Row {
            DpadRingBox(androidx.compose.material3.ButtonDefaults.filledTonalShape) {
                FilledTonalButton(
                    modifier = Modifier.dpadRowSibling(projFocus, 0),
                    onClick = { open(app.vela.core.config.VelaLinks.site()) },
                ) { Text(stringResource(R.string.settings_project_site_button)) }
            }
            Spacer(Modifier.width(8.dp))
            DpadRingBox(androidx.compose.material3.ButtonDefaults.filledTonalShape) {
                FilledTonalButton(
                    modifier = Modifier.dpadRowSibling(projFocus, 1),
                    onClick = { open(app.vela.core.config.VelaLinks.repo()) },
                ) { Text(stringResource(R.string.settings_project_github_button)) }
            }
        }
        }
        }

        Spacer(Modifier.height(8.dp))
        // Where the map comes from (issue #302): the ODbL credit belongs in About as well as on
        // the map, and people asked where the streets come from when they differ from Google.
        SettingsGroup(title = stringResource(R.string.settings_map_data)) {
        androidx.compose.foundation.layout.Column(Modifier.padding(horizontal = 16.dp)) {
        Hint(stringResource(R.string.settings_map_data_hint))
        Spacer(Modifier.height(4.dp))
        FilledTonalButton(
            modifier = Modifier.dpadHighlight(androidx.compose.foundation.shape.CircleShape),
            onClick = {
                runCatching {
                    context.startActivity(
                        android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.openstreetmap.org/copyright")),
                    )
                }
            },
        ) {
            Text(stringResource(R.string.settings_map_data_button))
        }
        Spacer(Modifier.height(4.dp))
        FilledTonalButton(
            modifier = Modifier.dpadHighlight(androidx.compose.foundation.shape.CircleShape),
            onClick = {
                runCatching {
                    context.startActivity(
                        android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://cdla.dev/permissive-2-0/")),
                    )
                }
            },
        ) {
            Text(stringResource(R.string.settings_map_data_overture_button))
        }
        // UK fuel prices (SPEC 5.8): the Open Government Licence asks for its attribution line.
        Spacer(Modifier.height(4.dp))
        Hint(stringResource(R.string.settings_map_data_fuel_gb))
        Spacer(Modifier.height(4.dp))
        FilledTonalButton(
            modifier = Modifier.dpadHighlight(androidx.compose.foundation.shape.CircleShape),
            onClick = {
                runCatching {
                    context.startActivity(
                        android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/")),
                    )
                }
            },
        ) {
            Text(stringResource(R.string.settings_map_data_ogl_button))
        }
        }
        }

        Spacer(Modifier.height(8.dp))
        SettingsGroup(title = stringResource(R.string.settings_version)) {
        // Tap to copy - bug reports need the exact version, and typing it from the screen
        // is error-prone (issue #58's one keeper suggestion).
        val versionLine = stringResource(R.string.settings_version_line, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
        val versionCopied = stringResource(R.string.settings_version_copied)
        val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
        Text(
            versionLine,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .dpadHighlight()
                .clickable {
                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(versionLine))
                    android.widget.Toast.makeText(context, versionCopied, android.widget.Toast.LENGTH_SHORT).show()
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
        GroupDivider()
        // Who Android says installed this build. Android Auto only lists a navigation app whose
        // install source is Play, which King Installer and AAEnabler set on purpose, and a
        // self-update quietly resets; this row says whether that setup is still in place, so it
        // can be checked BEFORE an update and after one (user 2026-09-22, ahead of a GrapheneOS
        // install).
        val installer = remember { app.vela.update.InstallSource.installingPackage(context) }
        // Who STARTED the install is its own record, and the one newer Android Auto reads; shown
        // when it differs, so a setup that set only one of the two can be seen for what it is.
        val initiator = remember { app.vela.update.InstallSource.initiatingPackage(context) }
        val installerLine = when {
            app.vela.update.InstallSource.setForCar(context) -> stringResource(R.string.settings_installer_play, installer ?: initiator ?: "")
            installer.isNullOrBlank() -> stringResource(R.string.settings_installer_none)
            else -> stringResource(R.string.settings_installer_other, installer)
        } + if (!initiator.isNullOrBlank() && initiator != installer) " " + stringResource(R.string.settings_installer_started_by, initiator) else ""
        Text(
            installerLine,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        GroupDivider()
        // The release notes of this build, on demand (they also show once after an update).
        Text(
            stringResource(R.string.settings_whatsnew),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .dpadHighlight()
                .clickable { app.vela.ui.WhatsNew.show(context, force = true) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
        Hint(stringResource(R.string.settings_whatsnew_hint))
        }
        } // about
        Spacer(Modifier.height(8.dp))
        if (page == "updates") {
        // AutoMap updates (2026-10-27): the AutoSpace UpdateChecker pattern,
        // ported as AutoMapUpdater and pointed at DocBobert6/AutoMap. Alpha is
        // the default channel for alpha builds. No group title here (audit
        // A-09): the page itself is already titled "AutoMap updates".
        SettingsGroup {
            val updState by app.vela.update.AutoMapUpdater.state.collectAsStateWithLifecycle()
            val updContext = LocalContext.current
            var updAuto by remember { mutableStateOf(app.vela.update.AutoMapUpdater.autoCheckEnabled(updContext)) }
            ToggleRow(
                label = stringResource(R.string.settings_automap_update_auto),
                checked = updAuto,
                onCheckedChange = { updAuto = it; app.vela.update.AutoMapUpdater.setAutoCheckEnabled(updContext, it) },
                hint = stringResource(R.string.settings_automap_update_auto_hint),
            )
            GroupDivider()
            // What's New after an update (audit A-09, moved here from About):
            // the toggle belongs with the updater that triggers the notes.
            ToggleRow(
                label = stringResource(R.string.whatsnew_show_after_updates),
                checked = app.vela.ui.WhatsNew.enabled.value,
                onCheckedChange = { app.vela.ui.WhatsNew.setEnabled(updContext, it) },
                hint = stringResource(R.string.settings_whatsnew_toggle_hint),
            )
            GroupDivider()
            var updChannel by remember { mutableStateOf(app.vela.update.AutoMapUpdater.channel(updContext)) }
            Text(
                stringResource(R.string.settings_automap_update_channel),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            listOf(
                app.vela.update.AutoMapUpdater.Channel.STABLE to stringResource(R.string.settings_automap_update_channel_stable),
                app.vela.update.AutoMapUpdater.Channel.BETA to stringResource(R.string.settings_automap_update_channel_beta),
                app.vela.update.AutoMapUpdater.Channel.ALPHA to stringResource(R.string.settings_automap_update_channel_alpha),
            ).forEach { (id, label) ->
                SelectableRow(
                    label = label,
                    selected = updChannel == id,
                    onClick = { updChannel = id; app.vela.update.AutoMapUpdater.setChannel(updContext, id) },
                )
            }
            Hint(stringResource(R.string.settings_automap_update_channel_hint))
            GroupDivider()
            Spacer(Modifier.height(14.dp))
            when (val st = updState) {
                is app.vela.update.AutoMapUpdater.State.Checking -> Hint(stringResource(R.string.settings_automap_update_checking))
                is app.vela.update.AutoMapUpdater.State.UpToDate -> Hint(stringResource(R.string.settings_automap_update_none))
                is app.vela.update.AutoMapUpdater.State.Available -> {
                    Text(
                        stringResource(R.string.settings_automap_update_available, st.release.version),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    DpadRingBox(androidx.compose.material3.ButtonDefaults.filledTonalShape, Modifier.padding(horizontal = 16.dp)) {
                        FilledTonalButton(onClick = { app.vela.update.AutoMapUpdater.download(updContext, st.release) }) {
                            Text(stringResource(R.string.settings_automap_update_download))
                        }
                    }
                }
                is app.vela.update.AutoMapUpdater.State.Downloading -> {
                    Text(
                        stringResource(R.string.settings_automap_update_downloading, (st.fraction * 100).toInt()),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    app.vela.ui.VelaProgressBar(st.fraction, Modifier.padding(horizontal = 16.dp))
                }
                is app.vela.update.AutoMapUpdater.State.Ready -> {
                    Text(
                        stringResource(R.string.settings_automap_update_available, st.release.version),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    DpadRingBox(androidx.compose.material3.ButtonDefaults.filledTonalShape, Modifier.padding(horizontal = 16.dp)) {
                        FilledTonalButton(onClick = { app.vela.update.AutoMapUpdater.install(updContext) }) {
                            Text(stringResource(R.string.settings_automap_update_install))
                        }
                    }
                    // The About page's installer row warns an in-app update undoes an
                    // Android Auto setup; repeat it here, at the Install button, so the
                    // updater page does not contradict it (audit #22).
                    if (app.vela.update.InstallSource.setForCar(updContext)) {
                        Hint(stringResource(R.string.settings_installer_play, app.vela.update.InstallSource.installingPackage(updContext) ?: app.vela.update.InstallSource.initiatingPackage(updContext) ?: ""))
                    }
                }
                is app.vela.update.AutoMapUpdater.State.Failed -> Hint(stringResource(R.string.settings_automap_update_failed, st.message))
                else -> {}
            }
            Spacer(Modifier.height(10.dp))
            DpadRingBox(androidx.compose.material3.ButtonDefaults.filledTonalShape, Modifier.padding(horizontal = 16.dp)) {
                FilledTonalButton(onClick = { app.vela.update.AutoMapUpdater.check(updContext) }) {
                    Text(stringResource(R.string.settings_automap_update_check_now))
                }
            }
            Hint(stringResource(R.string.settings_automap_update_caveat))
            Spacer(Modifier.height(10.dp))
        }
        } // updates
        // Breathing room under the last control so the button doesn't sit right on the
        // gesture bar at the end of the scroll.
        Spacer(Modifier.height(56.dp))
    }
}
