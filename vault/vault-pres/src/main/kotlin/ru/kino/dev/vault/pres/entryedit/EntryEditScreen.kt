package ru.kino.dev.vault.pres.entryedit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ru.kino.dev.core.pres.R
import ru.kino.dev.core.preset.PresetField
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.ui.components.Dropdown
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.PasswordField
import ru.kino.dev.ui.components.SecondaryButton
import ru.kino.dev.ui.components.SnackbarMessage
import ru.kino.dev.ui.components.TextField
import ru.kino.dev.ui.components.skeletonBrush
import ru.kino.dev.vault.entry.EntryContent
import ru.kino.dev.vault.pres.common.EntryPresetSection
import ru.kino.dev.vault.pres.common.EntrySection
import ru.kino.dev.vault.pres.common.EntrySkeleton
import ru.kino.dev.vault.pres.common.EntryTitleSection
import ru.kino.dev.vault.pres.common.EntryTopBar
import ru.kino.dev.vault.pres.common.fieldLabel
import ru.kino.dev.vault.pres.common.option
import ru.kino.dev.vault.pres.common.presetName
import ru.kino.dev.vault.pres.entryedit.components.EntryEditDialogs
import ru.kino.dev.vault.pres.entryedit.components.OwnFieldRow
import ru.kino.dev.vault.pres.entryedit.components.nameProblemText

/**
 * Creating an entry inside [groupId], or editing [entryId] - exactly one of them is given.
 */
@Composable
internal fun EntryEditScreen(
    dbKey: String,
    groupId: String?,
    entryId: String?,
    processor: EntryEditProcessor = hiltViewModel<EntryEditProcessor, EntryEditProcessor.Factory>(
        creationCallback = { factory -> factory.create(dbKey, groupId, entryId) },
    ),
) {
    val state by processor.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    // The system "back" asks as the one in the top bar does
    BackHandler { processor.action(EntryEditAction.Back) }

    val message = state.message?.let {
        stringResource(
            when (it) {
                EntryEditMessage.LoadFailed -> R.string.entry_message_load_failed
                EntryEditMessage.Unexpected -> R.string.error_unexpected
            },
        )
    }
    SnackbarMessage(message, snackbar) { processor.action(EntryEditAction.MessageShown) }

    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                EntryTopBar(
                    title = stringResource(if (state.creating) R.string.entry_title_new else R.string.entry_title_edit),
                    onBack = { processor.action(EntryEditAction.Back) },
                )
            },
            floatingActionButton = {
                if (state.content != null) {
                    SaveFab(saving = state.saving, onSave = { processor.action(EntryEditAction.Save) })
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
                val content = state.content
                when {
                    state.loading -> EntrySkeleton(brush = skeletonBrush())
                    content != null -> EntryForm(state = state, content = content, action = processor::action)
                }
            }
        }

        EntryEditDialogs(
            dialog = state.dialog,
            onNameChange = { processor.action(EntryEditAction.DialogNameChanged(it)) },
            onConfirm = { processor.action(EntryEditAction.DialogConfirm) },
            onDismiss = { processor.action(EntryEditAction.DialogDismiss) },
            onPresetChosen = { processor.action(EntryEditAction.PresetChosen(it)) },
        )
    }
}

@Composable
private fun EntryForm(state: EntryEditState, content: EntryContent, action: (EntryEditAction) -> Unit) {
    val enabled = !state.saving
    val missing = stringResource(R.string.entry_field_required)

    @Composable
    fun PresetText(field: PresetField, modifier: Modifier = Modifier.fillMaxWidth()) {
        val key = field.key
        val notes = key == StandardPreset.NOTES
        TextField(
            value = content.value(key),
            onValueChange = { action(EntryEditAction.ValueChanged(key, it)) },
            label = fieldLabel(key),
            modifier = modifier,
            required = field.required,
            error = if (state.isMissing(key)) missing else null,
            enabled = enabled,
            singleLine = !notes,
            minLines = if (notes) NOTES_LINES else 1,
            keyboardOptions = keyboardFor(key),
        )
    }

    @Composable
    fun PresetInput(field: PresetField) {
        if (field.key == StandardPreset.PASSWORD) {
            PasswordField(
                value = content.value(field.key),
                onValueChange = { action(EntryEditAction.ValueChanged(field.key, it)) },
                label = fieldLabel(field.key),
                visible = state.passwordVisible,
                onToggleVisible = { action(EntryEditAction.TogglePassword) },
                modifier = Modifier.fillMaxWidth(),
                required = field.required,
                error = if (state.isMissing(field.key)) missing else null,
                enabled = enabled,
                imeAction = ImeAction.Next,
            )
        } else {
            PresetText(field)
        }
    }

    EntryTitleSection(enabled = enabled) { modifier ->
        PresetText(field = content.preset.fields.first(), modifier = modifier)
    }

    EntrySection {
        Dropdown(
            selected = state.group,
            options = state.groups.map { it.option() },
            onSelect = { action(EntryEditAction.GroupChosen(it)) },
            label = stringResource(R.string.entry_field_group),
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
        )
    }

    EntryPresetSection(
        name = presetName(content.preset),
        onClick = { action(EntryEditAction.ChoosePreset) },
        enabled = enabled,
    )

    // The cards of the preset, as the preset has them
    content.preset.cards.forEach { card ->
        EntrySection {
            card.fields.forEach { PresetInput(it) }
        }
    }

    // A card of its own: the own fields - read or added here - and the button that adds one
    EntrySection {
        content.ownFields.forEachIndexed { index, field ->
            OwnFieldRow(
                field = field,
                shown = index in state.shownOwn,
                error = state.nameProblem(index)?.let { nameProblemText(it) },
                enabled = enabled,
                onValueChange = { action(EntryEditAction.OwnValueChanged(index, it)) },
                onToggleShown = { action(EntryEditAction.ToggleOwn(index)) },
                onRename = { action(EntryEditAction.RenameField(index)) },
                onDelete = { action(EntryEditAction.DeleteField(index)) },
            )
        }

        SecondaryButton(
            text = stringResource(R.string.entry_add_field),
            onClick = { action(EntryEditAction.AddField) },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
        )
    }
}

/** The check that saves; while the change is made and written, it waits and takes no taps. */
@Composable
private fun SaveFab(saving: Boolean, onSave: () -> Unit) {
    FloatingActionButton(onClick = { if (!saving) onSave() }) {
        if (saving) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current,
            )
        } else {
            Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.entry_save))
        }
    }
}

/**
 * The keyboard of a field of a preset: the standard ones each their own, a field the person named plain
 * text. Notes - the last of every preset - leave "next" to the line break.
 */
private fun keyboardFor(key: String): KeyboardOptions = when (key) {
    StandardPreset.TITLE, StandardPreset.SERVICE ->
        KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next)
    StandardPreset.USER_NAME -> KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next)
    StandardPreset.URL ->
        KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false, imeAction = ImeAction.Next)
    StandardPreset.NOTES -> KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
    else -> KeyboardOptions(imeAction = ImeAction.Next)
}

/** Room around the cards; at the bottom - for the fab, so it never covers the last field. */
private val CONTENT_PADDING = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp)

/** Notes start this tall and grow with the text. */
private const val NOTES_LINES = 3
