package ru.kino.dev.vault.pres.presetcreate

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
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.SecondaryButton
import ru.kino.dev.ui.components.SnackbarMessage
import ru.kino.dev.ui.components.TextField
import ru.kino.dev.vault.pres.common.EntrySection
import ru.kino.dev.vault.pres.common.EntryTopBar
import ru.kino.dev.vault.pres.common.SaveFab
import ru.kino.dev.vault.pres.common.fieldLabel
import ru.kino.dev.vault.pres.presetcreate.components.CardDeleteBadge
import ru.kino.dev.vault.pres.presetcreate.components.PresetCreateDialogs
import ru.kino.dev.vault.pres.presetcreate.components.PresetFieldRow
import ru.kino.dev.vault.pres.presetcreate.components.fieldNameProblemText
import ru.kino.dev.vault.pres.presetcreate.components.presetNameProblemText

/** Creating a preset of one's own (pass-docs, vault-our.md "Экран создания пресетов"). */
@Composable
internal fun PresetCreateScreen(processor: PresetCreateProcessor = hiltViewModel()) {
    val state by processor.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val standardName = stringResource(R.string.entry_preset_standard)

    // The system "back" asks as the one in the top bar does
    BackHandler { processor.action(PresetCreateAction.Back) }

    val message = state.message?.let {
        when (it) {
            PresetCreateMessage.TooMany -> pluralStringResource(
                R.plurals.preset_too_many,
                PresetLimits.MAX_PRESETS,
                PresetLimits.MAX_PRESETS,
            )
            PresetCreateMessage.NotSaved -> stringResource(R.string.preset_not_saved)
        }
    }
    SnackbarMessage(message, snackbar) { processor.action(PresetCreateAction.MessageShown) }

    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                EntryTopBar(
                    title = stringResource(R.string.preset_create_title),
                    onBack = { processor.action(PresetCreateAction.Back) },
                )
            },
            floatingActionButton = {
                SaveFab(saving = state.saving, onSave = { processor.action(PresetCreateAction.Save(standardName)) })
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

        PresetCreateDialogs(
            dialog = state.dialog,
            onNameChange = { processor.action(PresetCreateAction.DialogNameChanged(it)) },
            onConfirm = { processor.action(PresetCreateAction.DialogConfirm) },
            onDismiss = { processor.action(PresetCreateAction.DialogDismiss) },
        )
    }
}

@Composable
private fun PresetForm(state: PresetCreateState, action: (PresetCreateAction) -> Unit) {
    val enabled = !state.saving

    // The name of the preset - the first card, required
    EntrySection {
        TextField(
            value = state.name,
            onValueChange = { action(PresetCreateAction.NameChanged(it)) },
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
                    onClick = { action(PresetCreateAction.DeleteCard(card)) },
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
    }

    SecondaryButton(
        text = stringResource(R.string.preset_add_card),
        onClick = { action(PresetCreateAction.AddCard) },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
    )

    // The notes close every preset: a card of their own, fixed
    EntrySection {
        PresetFieldRow(name = fieldLabel(StandardPreset.NOTES), enabled = enabled)
    }
}

/** A card of the preset: its fields and "+ add field" under them. */
@Composable
private fun PresetCard(
    card: Int,
    keys: List<String>,
    state: PresetCreateState,
    enabled: Boolean,
    action: (PresetCreateAction) -> Unit,
) {
    EntrySection {
        if (card == 0) {
            // The first card holds the user name and the password - in every preset, not to be removed
            PresetFieldRow(name = fieldLabel(StandardPreset.USER_NAME), enabled = enabled, required = true)
            PresetFieldRow(name = fieldLabel(StandardPreset.PASSWORD), enabled = enabled, required = true)
        }

        keys.forEachIndexed { index, key ->
            val at = FieldPosition(card, index)
            PresetFieldRow(
                name = key,
                enabled = enabled,
                error = state.fieldProblem(at)?.let { fieldNameProblemText(it) },
                onRename = { action(PresetCreateAction.RenameField(at)) },
                onDelete = { action(PresetCreateAction.DeleteField(at)) },
            )
        }

        SecondaryButton(
            text = stringResource(R.string.entry_add_field),
            onClick = { action(PresetCreateAction.AddField(card)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
        )
    }
}

/** Room around the cards; at the bottom - for the fab, so it never covers the last card. */
private val CONTENT_PADDING = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp)
