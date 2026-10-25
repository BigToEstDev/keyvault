package ru.kino.dev.settings.pres.presetedit

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ru.kino.dev.core.pres.R
import ru.kino.dev.core.preset.FieldPosition
import ru.kino.dev.core.preset.PresetLimits
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.settings.pres.presetedit.components.CardDeleteBadge
import ru.kino.dev.settings.pres.presetedit.components.PresetEditDialogs
import ru.kino.dev.settings.pres.presetedit.components.PresetFieldRow
import ru.kino.dev.settings.pres.presetedit.components.fieldNameProblemText
import ru.kino.dev.settings.pres.presetedit.components.presetNameProblemText
import ru.kino.dev.ui.components.BackTopBar
import ru.kino.dev.ui.components.FieldSection
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.SECTION_PADDING
import ru.kino.dev.ui.components.SaveFab
import ru.kino.dev.ui.components.SecondaryButton
import ru.kino.dev.ui.components.SnackbarMessage
import ru.kino.dev.ui.components.TextField
import ru.kino.dev.ui.preset.fieldLabel

/**
 * Creating a preset of one's own, or editing the one of [presetId] (pass-docs, vault-our.md "Экран создания
 * пресетов", Step 62).
 */
@Composable
internal fun PresetEditScreen(
    presetId: String?,
    processor: PresetEditProcessor = hiltViewModel<PresetEditProcessor, PresetEditProcessor.Factory>(
        key = presetId,
        creationCallback = { factory -> factory.create(presetId) },
    ),
) {
    val state by processor.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val standardName = stringResource(R.string.entry_preset_standard)

    LaunchedEffect(Unit) { processor.action(PresetEditAction.Load) }

    // The system "back" asks as the one in the top bar does
    BackHandler { processor.action(PresetEditAction.Back) }

    val message = state.message?.let {
        when (it) {
            PresetEditMessage.TooMany -> pluralStringResource(
                R.plurals.preset_too_many,
                PresetLimits.MAX_PRESETS,
                PresetLimits.MAX_PRESETS,
            )
            PresetEditMessage.NotSaved -> stringResource(R.string.preset_not_saved)
            PresetEditMessage.Gone -> stringResource(R.string.preset_gone)
        }
    }
    SnackbarMessage(message, snackbar) { processor.action(PresetEditAction.MessageShown) }

    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                BackTopBar(
                    title = stringResource(if (state.editing) R.string.preset_edit_title else R.string.preset_create_title),
                    onBack = { processor.action(PresetEditAction.Back) },
                )
            },
            floatingActionButton = {
                // While the preset to edit is read the fab waits as while it is saved
                SaveFab(saving = state.saving || state.loading, onSave = { processor.action(PresetEditAction.Save(standardName)) })
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
                PresetForm(state = state, action = processor::action)
            }
        }

        PresetEditDialogs(
            dialog = state.dialog,
            onNameChange = { processor.action(PresetEditAction.DialogNameChanged(it)) },
            onProtectedChange = { processor.action(PresetEditAction.DialogProtectedChanged(it)) },
            onConfirm = { processor.action(PresetEditAction.DialogConfirm) },
            onDismiss = { processor.action(PresetEditAction.DialogDismiss) },
        )
    }
}

@Composable
private fun PresetForm(state: PresetEditState, action: (PresetEditAction) -> Unit) {
    val enabled = !state.saving && !state.loading

    // The name of the preset - the first card, required
    FieldSection {
        TextField(
            value = state.name,
            onValueChange = { action(PresetEditAction.NameChanged(it)) },
            label = stringResource(R.string.preset_name),
            modifier = Modifier.fillMaxWidth(),
            required = true,
            error = state.nameProblem?.let { presetNameProblemText(it) },
            enabled = enabled,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        )
    }

    state.cards.forEachIndexed { card, keys ->
        Box {
            PresetCard(card = card, keys = keys, state = state, enabled = enabled, action = action)
            if (card > 0) {
                // A card the person added: the red badge on its corner deletes it, asking first
                CardDeleteBadge(
                    contentDescription = stringResource(R.string.preset_delete_card),
                    enabled = enabled,
                    onClick = { action(PresetEditAction.DeleteCard(card)) },
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
    }

    // Past the most cards a preset takes there is no button; a card deleted brings it back
    if (state.canAddCard) {
        SecondaryButton(
            text = stringResource(R.string.preset_add_card),
            onClick = { action(PresetEditAction.AddCard) },
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
private fun PresetCard(
    card: Int,
    keys: List<String>,
    state: PresetEditState,
    enabled: Boolean,
    action: (PresetEditAction) -> Unit,
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
                onEdit = { action(PresetEditAction.EditField(at)) },
                onDelete = { action(PresetEditAction.DeleteField(at)) },
            )
        }

        // Past the most fields a card takes there is no button; a field deleted brings it back
        if (state.canAddField(card)) {
            SecondaryButton(
                text = stringResource(R.string.entry_add_field),
                onClick = { action(PresetEditAction.AddField(card)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
            )
        }
    }
}

/** Room around the cards; at the bottom - for the fab, so it never covers the last card. */
private val CONTENT_PADDING = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp)
