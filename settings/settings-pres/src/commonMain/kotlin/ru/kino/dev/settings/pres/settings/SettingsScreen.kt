package ru.kino.dev.settings.pres.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ru.kino.dev.core.pres.*
import ru.kino.dev.settings.pres.navigation.route
import ru.kino.dev.settings.pres.settings.components.SettingsCard
import ru.kino.dev.settings.pres.settings.components.SettingsSectionHeader
import ru.kino.dev.ui.components.BackTopBar
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.res.stringResource

/**
 * The settings: the cards of the app and of the database under their sections, "About" at the bottom, apart
 * behind a divider. A card opens its screen when the screen is made (pass-docs, Step 55).
 */
@Composable
internal fun SettingsScreen(
    dbKey: String,
    processor: SettingsProcessor = koinViewModel { parametersOf(dbKey) },
) {
    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                BackTopBar(
                    title = stringResource(Res.string.settings_title),
                    onBack = { processor.action(SettingsAction.Back) },
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SettingsSection.entries.forEach { section ->
                    // The first section starts right under the top bar, like the list of `vault`; the next is set apart
                    item(key = section) {
                        SettingsSectionHeader(
                            title = stringResource(section.title),
                            hint = stringResource(section.hint),
                            top = if (section.ordinal == 0) 0.dp else 8.dp,
                        )
                    }
                    items(SettingsItem.entries.filter { it.section == section }, key = { it }) { item ->
                        SettingsCard(
                            icon = item.icon,
                            title = stringResource(item.title),
                            text = stringResource(item.text),
                            onClick = { processor.action(SettingsAction.Open(item)) },
                        )
                    }
                }
                item(key = "divider") { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                item(key = "about") {
                    SettingsCard(icon = Icons.Filled.Info, title = stringResource(Res.string.settings_about))
                }
            }
        }
    }
}

