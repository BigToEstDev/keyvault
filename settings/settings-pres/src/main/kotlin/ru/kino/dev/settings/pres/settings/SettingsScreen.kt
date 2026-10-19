package ru.kino.dev.settings.pres.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import ru.kino.dev.core.pres.R
import ru.kino.dev.settings.pres.settings.components.SettingsCard
import ru.kino.dev.ui.components.BackTopBar
import ru.kino.dev.ui.components.GradientBackground

/**
 * The settings: the cards of the app and of the database under their sections, "About" at the bottom, apart
 * behind a divider. Nothing opens yet (pass-docs, Step 54).
 */
@Composable
internal fun SettingsScreen(processor: SettingsProcessor = hiltViewModel()) {
    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                BackTopBar(
                    title = stringResource(R.string.settings_title),
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
                    item(key = section) { SectionHeader(section, top = if (section.ordinal == 0) 0.dp else 8.dp) }
                    items(SettingsItem.entries.filter { it.section == section }, key = { it }) { item ->
                        SettingsCard(
                            icon = item.icon,
                            title = stringResource(item.title),
                            text = stringResource(item.text),
                        )
                    }
                }
                item(key = "divider") { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                item(key = "about") {
                    SettingsCard(icon = Icons.Filled.Info, title = stringResource(R.string.settings_about))
                }
            }
        }
    }
}

/** The name of a section and where its settings are kept, [top] apart from what is above. */
@Composable
private fun SectionHeader(section: SettingsSection, top: Dp) {
    Column(modifier = Modifier.padding(start = 4.dp, top = top)) {
        Text(
            text = stringResource(section.title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(section.hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
