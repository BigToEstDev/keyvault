package ru.kino.dev.settings.pres.presets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import org.orbitmvi.orbit.compose.collectAsState
import ru.kino.dev.core.pres.R
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.settings.pres.presets.components.PresetCard
import ru.kino.dev.settings.pres.presets.components.PresetCardSkeleton
import ru.kino.dev.settings.pres.presets.components.UsePresetsSkeleton
import ru.kino.dev.ui.components.BackTopBar
import ru.kino.dev.ui.components.ConfirmDialog
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.SnackbarMessage
import ru.kino.dev.ui.components.SwitchRow
import ru.kino.dev.ui.components.deleteMenuItem
import ru.kino.dev.ui.components.editMenuItem
import ru.kino.dev.ui.components.skeletonBrush
import ru.kino.dev.ui.preset.presetName

/**
 * The presets: "Use presets" on top; on - the standard preset, those of one's own with their pen - "Edit", "Delete" -
 * and the fab "Create" while there is room for one; off - the switch alone (pass-docs, Step 62).
 */
@Composable
internal fun PresetsScreen(processor: PresetsProcessor = hiltViewModel()) {
    val state by processor.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    // Back from a preset made or edited, the list is read again
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { processor.action(PresetsAction.Load) }

    val message = state.message?.let {
        stringResource(
            when (it) {
                PresetsMessage.NotSaved -> R.string.settings_message_not_saved
                PresetsMessage.NotDeleted -> R.string.preset_not_deleted
            },
        )
    }
    SnackbarMessage(message, snackbar) { processor.action(PresetsAction.MessageShown) }

    val shown = state.usePresets == true
    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                BackTopBar(
                    title = stringResource(R.string.settings_presets),
                    onBack = { processor.action(PresetsAction.Back) },
                )
            },
            floatingActionButton = {
                // Past the most presets a person has there is none; a preset deleted brings it back
                if (shown && state.canCreate) {
                    FloatingActionButton(onClick = { processor.action(PresetsAction.Create) }) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_create))
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = CONTENT_PADDING,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val usePresets = state.usePresets
                val own = state.presets
                if (usePresets == null || own == null) {
                    // Until read nothing is chosen for real: the shapes of the screen, not a switch that turns on
                    item(key = "skeleton") {
                        val brush = skeletonBrush()
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            UsePresetsSkeleton(brush)
                            PresetCardSkeleton(brush)
                        }
                    }
                    return@LazyColumn
                }
                item(key = "use-presets") {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        SwitchRow(
                            title = stringResource(R.string.settings_use_presets),
                            text = stringResource(R.string.settings_use_presets_hint),
                            checked = usePresets,
                            onCheckedChange = { processor.action(PresetsAction.SetUsePresets(it)) },
                        )
                    }
                }
                if (usePresets) {
                    item(key = "standard") {
                        PresetCard(name = presetName(StandardPreset.preset), menu = emptyList())
                    }
                    items(own, key = { it.id?.value.orEmpty() }) { preset ->
                        PresetCard(
                            name = presetName(preset),
                            menu = listOf(
                                editMenuItem { preset.id?.let { processor.action(PresetsAction.Edit(it)) } },
                                deleteMenuItem { processor.action(PresetsAction.Delete(preset)) },
                            ),
                        )
                    }
                }
            }
        }
    }

    state.deleting?.let { preset ->
        ConfirmDialog(
            title = stringResource(R.string.preset_delete_title, presetName(preset)),
            text = stringResource(R.string.preset_delete_text),
            confirmText = stringResource(R.string.action_delete),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = { processor.action(PresetsAction.DeleteConfirm) },
            onDismiss = { processor.action(PresetsAction.DeleteDismiss) },
            destructive = true,
        )
    }
}

/** Room around the cards; at the bottom - for the fab, so it never covers the last card. */
private val CONTENT_PADDING = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp)
