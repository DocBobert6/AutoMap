package app.vela.ui.settings.sections

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.vela.R
import app.vela.ui.settings.CategoryRow
import app.vela.ui.settings.GroupDivider
import app.vela.ui.settings.SettingsGroup
import app.vela.ui.settings.SettingsScaffold
import app.vela.ui.settings.ToggleRow

/**
 * Places sub-screen (was Place pages; 2026-09-17 settings reshuffle): where the map's places come
 * from, what the map draws for them, and the five place-page content toggles.
 */
@Composable
internal fun PlacesSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var page by remember { mutableStateOf<String?>(null) }
    if (page == null) {
        SettingsScaffold(stringResource(R.string.settings_places), onBack) { _ ->
            Spacer(Modifier.height(4.dp))
            CategoryRow(stringResource(R.string.settings_cat_places_map), stringResource(R.string.settings_cat_places_map_sub)) { page = "map" }
            CategoryRow(stringResource(R.string.settings_cat_place_pages), stringResource(R.string.settings_cat_place_pages_sub)) { page = "pages" }
            Spacer(Modifier.height(24.dp))
        }
        return
    }
    val pageTitle = if (page == "map") stringResource(R.string.settings_cat_places_map) else stringResource(R.string.settings_cat_place_pages)
    SettingsScaffold(pageTitle, { page = null }) { topRow ->
        Spacer(Modifier.height(4.dp))
        if (page == "map") {
        PlacesSourceGroup(topRow)
        Spacer(Modifier.height(8.dp))
        PlacesOnMapGroup()
        }
        if (page == "pages") {
        SettingsGroup(title = stringResource(R.string.settings_place_pages)) {
        FullPlaceLoadRow()
        GroupDivider()
        ReviewAndPhotoRows()
        GroupDivider()
        ToggleRow(
            label = stringResource(R.string.settings_hide_adult),
            checked = app.vela.ui.HideAdult.on.value,
            onCheckedChange = { app.vela.ui.HideAdult.set(context, it) },
            hint = stringResource(R.string.settings_hide_adult_hint),
        )
        GroupDivider()
        ToggleRow(
            label = stringResource(R.string.settings_hide_external_links),
            checked = app.vela.ui.HideExternalLinks.on.value,
            onCheckedChange = { app.vela.ui.HideExternalLinks.set(context, it) },
            hint = stringResource(R.string.settings_hide_external_links_hint),
        )
        }
        } // pages
        Spacer(Modifier.height(24.dp))
    }
}
