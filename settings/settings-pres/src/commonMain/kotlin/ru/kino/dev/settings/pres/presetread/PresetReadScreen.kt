package ru.kino.dev.settings.pres.presetread

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.orbitmvi.orbit.compose.collectAsState
import ru.kino.dev.core.pres.*
import ru.kino.dev.core.preset.EntryPreset
import ru.kino.dev.pane.koinProcessor
import ru.kino.dev.settings.pres.presetedit.components.PresetFieldRow
import ru.kino.dev.ui.components.BackTopBar
import ru.kino.dev.ui.components.ConfirmDialog
import ru.kino.dev.ui.components.FieldSection
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.MoreMenuButton
import ru.kino.dev.ui.components.SnackbarMessage
import ru.kino.dev.ui.components.deleteMenuItem
import ru.kino.dev.ui.components.skeletonBrush
import ru.kino.dev.ui.preset.fieldLabel
import ru.kino.dev.ui.preset.presetName
import ru.kino.dev.ui.res.stringResource

/**
 * A preset read (pass-docs, Step 99, item 8), as an entry is: its name on top, its cards with the names of their fields
 * as the form shows them, nothing to type. One's own - the fab "Edit" and "Delete" in the menu; the standard one -
 * neither.
 *
 * @param presetId one of one's own; null - the standard one
 * @param canGoBack false - a pane of the container of a wide window: its tab closes it, not "back"
 */
@Composable
internal fun PresetReadScreen(
    presetId: String?,
    canGoBack: Boolean = true,
    processor: PresetReadProcessor = koinProcessor(presetId),
) {
    val state by processor.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { processor.onLoad() }

    val message = state.message?.let {
        when (it) {
            PresetReadMessage.NotDeleted -> stringResource(Res.string.preset_not_deleted)
        }
    }
    SnackbarMessage(message, snackbar) { processor.onMessageShown() }

    val preset = state.preset
    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                BackTopBar(
                    title = preset?.let { presetName(it) }.orEmpty(),
                    onBack = processor::onBack,
                    back = canGoBack,
                    // Until read: the shape of the name
                    skeleton = if (preset == null) skeletonBrush() else null,
                ) {
                    if (state.own) {
                        MoreMenuButton(
                            contentDescription = stringResource(Res.string.preset_menu),
                            items = listOf(deleteMenuItem(processor::onDelete)),
                            enabled = !state.working,
                        )
                    }
                }
            },
            floatingActionButton = {
                if (state.own) {
                    FloatingActionButton(onClick = processor::onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(Res.string.action_edit))
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(CONTENT_PADDING),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (preset != null) PresetCards(preset)
            }
        }
    }

    if (state.deleting && preset != null) {
        ConfirmDialog(
            title = stringResource(Res.string.preset_delete_title, presetName(preset)),
            text = stringResource(Res.string.preset_delete_text),
            confirmText = stringResource(Res.string.action_delete),
            dismissText = stringResource(Res.string.action_cancel),
            onConfirm = processor::onDeleteConfirm,
            onDismiss = processor::onDeleteDismiss,
            destructive = true,
        )
    }
}

/** The cards of [preset], each with the names of its fields - the notes the last; required and protected marked. */
@Composable
private fun PresetCards(preset: EntryPreset) {
    preset.cards.forEach { card ->
        FieldSection {
            card.fields.forEach { field ->
                PresetFieldRow(
                    name = fieldLabel(field.key),
                    enabled = true,
                    modifier = Modifier.fillMaxWidth(),
                    required = field.required,
                    protected = field.protected,
                )
            }
        }
    }
}

/** Room around the cards; at the bottom - for the fab, so it never covers the last card. */
private val CONTENT_PADDING = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp)
