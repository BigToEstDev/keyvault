package ru.kino.dev.settings.pres.presetedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import org.orbitmvi.orbit.compose.collectAsState
import ru.kino.dev.core.pres.*
import ru.kino.dev.core.preset.FieldPosition
import ru.kino.dev.core.preset.PresetLimits
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.pane.koinProcessor
import ru.kino.dev.settings.pres.presetedit.components.CardDeleteBadge
import ru.kino.dev.settings.pres.presetedit.components.PresetEditDialogs
import ru.kino.dev.settings.pres.presetedit.components.PresetFieldRow
import ru.kino.dev.settings.pres.presetedit.components.fieldNameProblemText
import ru.kino.dev.settings.pres.presetedit.components.presetNameProblemText
import ru.kino.dev.ui.components.BackHandler
import ru.kino.dev.ui.components.BackTopBar
import ru.kino.dev.ui.components.PaneCloseHandler
import ru.kino.dev.ui.components.FieldSection
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.MoreMenuButton
import ru.kino.dev.ui.components.SECTION_PADDING
import ru.kino.dev.ui.components.SaveFab
import ru.kino.dev.ui.components.SecondaryButton
import ru.kino.dev.ui.components.SnackbarMessage
import ru.kino.dev.ui.components.TextField
import ru.kino.dev.ui.components.deleteMenuItem
import ru.kino.dev.ui.preset.fieldLabel
import ru.kino.dev.ui.res.pluralStringResource
import ru.kino.dev.ui.res.stringResource

/**
 * Creating a preset of one's own, or editing the one of [presetId] (pass-docs, vault-our.md "Экран создания
 * пресетов", Step 62).
 *
 * @param readSaved a new one made - its reading in place of the form (pass-docs, Step 99, item 8)
 * @param canGoBack false - a pane of the container of a wide window: its tab closes it, not "back" (pass-docs, Step 100)
 */
@Composable
internal fun PresetEditScreen(
    presetId: String?,
    readSaved: Boolean = false,
    canGoBack: Boolean = true,
    processor: PresetEditProcessor = koinProcessor(presetId, readSaved, key = presetId),
) {
    val state by processor.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val standardName = stringResource(Res.string.entry_preset_standard)

    LaunchedEffect(Unit) { processor.onLoad() }

    // The system "back" asks as the one in the top bar does
    BackHandler { processor.onBack() }
    // The cross of its tab, in a container, asks as "back" does - and closes the tab, past the reading
    PaneCloseHandler(onClose = processor::onClose)

    val message = state.message?.let {
        when (it) {
            PresetEditMessage.TooMany -> pluralStringResource(
                Res.plurals.preset_too_many,
                PresetLimits.MAX_PRESETS,
                PresetLimits.MAX_PRESETS,
            )
            PresetEditMessage.NotSaved -> stringResource(Res.string.preset_not_saved)
            PresetEditMessage.Gone -> stringResource(Res.string.preset_gone)
            PresetEditMessage.NotDeleted -> stringResource(Res.string.preset_not_deleted)
        }
    }
    SnackbarMessage(message, snackbar) { processor.onMessageShown() }

    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                BackTopBar(
                    title = stringResource(if (state.editing) Res.string.preset_edit_title else Res.string.preset_create_title),
                    onBack = processor::onBack,
                    back = canGoBack,
                ) {
                    // A preset that is there, once read; a new one has nothing to delete
                    if (state.editing && !state.loading) {
                        MoreMenuButton(
                            contentDescription = stringResource(Res.string.preset_menu),
                            items = listOf(deleteMenuItem(processor::onDelete)),
                            enabled = !state.saving,
                        )
                    }
                }
            },
            floatingActionButton = {
                // While the preset to edit is read the fab waits as while it is saved
                SaveFab(saving = state.saving || state.loading, onSave = { processor.onSave(standardName) })
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
                processor.PresetForm(state = state)
            }
        }

        PresetEditDialogs(
            dialog = state.dialog,
            onNameChange = processor::onDialogNameChange,
            onProtectedChange = processor::onDialogProtectedChange,
            onTypeChange = processor::onDialogTypeChange,
            onConfirm = processor::onDialogConfirm,
            onDismiss = processor::onDialogDismiss,
        )
    }
}

@Composable
private fun PresetEditActions.PresetForm(state: PresetEditState) {
    val enabled = !state.saving && !state.loading

    // The name of the preset - the first card, required
    FieldSection {
        TextField(
            value = state.name,
            onValueChange = { onNameChange(it) },
            label = stringResource(Res.string.preset_name),
            modifier = Modifier.fillMaxWidth(),
            required = true,
            error = state.nameProblem?.let { presetNameProblemText(it) },
            enabled = enabled,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        )
    }

    state.cards.forEachIndexed { card, keys ->
        Box {
            PresetCard(card = card, keys = keys, state = state, enabled = enabled)
            if (card > 0) {
                // A card the person added: the red badge on its corner deletes it, asking first
                CardDeleteBadge(
                    contentDescription = stringResource(Res.string.preset_delete_card),
                    enabled = enabled,
                    onClick = { onDeleteCard(card) },
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
    }

    // Past the most cards a preset takes there is no button; a card deleted brings it back
    if (state.canAddCard) {
        SecondaryButton(
            text = stringResource(Res.string.preset_add_card),
            onClick = { onAddCard() },
            // As wide as "+ add field" in the cards above: inside the room of a card
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SECTION_PADDING),
            enabled = enabled,
        )
    }

    // The notes close every preset: a card of their own, fixed
    FieldSection {
        PresetFieldRow(name = fieldLabel(StandardPreset.NOTES), enabled = enabled)
    }
}

/** A card of the preset: its fields and "+ add field" under them. */
@Composable
private fun PresetEditActions.PresetCard(
    card: Int,
    keys: List<String>,
    state: PresetEditState,
    enabled: Boolean,
) {
    FieldSection {
        if (card == 0) {
            // The first card holds the user name and the password - in every preset, not to be removed
            PresetFieldRow(name = fieldLabel(StandardPreset.USER_NAME), enabled = enabled, required = true)
            PresetFieldRow(name = fieldLabel(StandardPreset.PASSWORD), enabled = enabled, required = true, protected = true)
        }

        keys.forEachIndexed { index, key ->
            val at = FieldPosition(card, index)
            PresetFieldRow(
                name = key,
                enabled = enabled,
                protected = key in state.protectedKeys,
                error = state.fieldProblem(at)?.let { fieldNameProblemText(it) },
                onEdit = { onEditField(at) },
                onDelete = { onDeleteField(at) },
            )
        }

        // Past the most fields a card takes there is no button; a field deleted brings it back
        if (state.canAddField(card)) {
            SecondaryButton(
                text = stringResource(Res.string.entry_add_field),
                onClick = { onAddField(card) },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
            )
        }
    }
}

/** Room around the cards; at the bottom - for the fab, so it never covers the last card. */
private val CONTENT_PADDING = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp)
