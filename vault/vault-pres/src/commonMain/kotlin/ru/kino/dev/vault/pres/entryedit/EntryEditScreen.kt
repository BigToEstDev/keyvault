package ru.kino.dev.vault.pres.entryedit

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.orbitmvi.orbit.compose.collectAsState
import ru.kino.dev.core.FieldType
import ru.kino.dev.core.pres.*
import ru.kino.dev.core.preset.PresetField
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.ui.components.BackHandler
import ru.kino.dev.ui.components.Dropdown
import ru.kino.dev.ui.components.FieldSection
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.PasswordField
import ru.kino.dev.ui.components.PhoneField
import ru.kino.dev.ui.components.PickerField
import ru.kino.dev.ui.components.SaveFab
import ru.kino.dev.ui.components.SecondaryButton
import ru.kino.dev.ui.components.SnackbarMessage
import ru.kino.dev.ui.components.TextField
import ru.kino.dev.ui.components.VisibilityToggle
import ru.kino.dev.ui.components.deleteMenuItem
import ru.kino.dev.ui.components.skeletonBrush
import ru.kino.dev.ui.phone.LocalPhoneNumbers
import ru.kino.dev.ui.phone.phoneHint
import ru.kino.dev.ui.phone.phonePrefix
import ru.kino.dev.ui.phone.rememberPhoneFormat
import ru.kino.dev.ui.preset.fieldLabel
import ru.kino.dev.ui.preset.presetName
import ru.kino.dev.ui.res.stringResource
import ru.kino.dev.ui.theme.LocalAppDisplay
import ru.kino.dev.vault.entry.EntryContent
import ru.kino.dev.vault.pres.common.EntrySkeleton
import ru.kino.dev.vault.pres.common.EntryTitleSection
import ru.kino.dev.vault.pres.common.EntryTopBar
import ru.kino.dev.vault.pres.common.hides
import ru.kino.dev.vault.pres.common.option
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
    processor: EntryEditProcessor = koinViewModel {
        parametersOf(EntryEditArgs(dbKey = dbKey, groupId = groupId, entryId = entryId))
    },
) {
    val state by processor.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    // The system "back" asks as the one in the top bar does
    BackHandler(onBack = processor::onBack)

    // Back from the screen of a new preset: the form picks it
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { processor.onResumed() }

    val message = state.message?.let {
        stringResource(
            when (it) {
                EntryEditMessage.LoadFailed -> Res.string.entry_message_load_failed
                EntryEditMessage.NotSaved -> Res.string.message_file_not_saved
                EntryEditMessage.Unexpected -> Res.string.error_unexpected
            },
        )
    }
    SnackbarMessage(message, snackbar) { processor.onMessageShown() }

    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                EntryTopBar(
                    title = stringResource(if (state.creating) Res.string.entry_title_new else Res.string.entry_title_edit),
                    onBack = processor::onBack,
                    // An entry that is there, once read; a new one has nothing to delete
                    menu = if (!state.creating && state.content != null) {
                        listOf(deleteMenuItem(processor::onDelete))
                    } else {
                        emptyList()
                    },
                    menuEnabled = !state.saving,
                )
            },
            floatingActionButton = {
                if (state.content != null) {
                    SaveFab(saving = state.saving, onSave = processor::onSave)
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
                    content != null -> processor.EntryForm(state = state, content = content)
                }
            }
        }

        EntryEditDialogs(
            dialog = state.dialog,
            onNameChange = processor::onDialogNameChange,
            onProtectedChange = processor::onDialogProtectedChange,
            onTypeChange = processor::onDialogTypeChange,
            onConfirm = processor::onDialogConfirm,
            onDismiss = processor::onDialogDismiss,
            onPresetChosen = processor::onPresetChosen,
            onCreatePreset = processor::onCreatePreset,
            onKeepHidden = processor::onKeepHidden,
            onPhoneCountryChosen = processor::onPhoneCountryChosen,
        )
    }
}

@Composable
private fun EntryEditActions.EntryForm(state: EntryEditState, content: EntryContent) {
    val enabled = !state.saving
    val missing = stringResource(Res.string.entry_field_required)

    @Composable
    fun PresetText(field: PresetField, modifier: Modifier = Modifier.fillMaxWidth()) {
        val key = field.key
        val notes = key == StandardPreset.NOTES
        // Private by the database: behind dots with the eye, as the password (pass-docs, Step 65)
        val private = state.protection.protects(key)
        val shown = key in state.shownKeys
        val hidden = private && !shown
        TextField(
            value = content.value(key),
            onValueChange = { onValueChange(key, it) },
            label = fieldLabel(key),
            modifier = modifier,
            required = field.required,
            error = if (state.isMissing(key)) missing else null,
            enabled = enabled,
            // A field the person named wraps and grows as notes do, from one line; behind dots - in one line
            // (pass-docs, Step 75, item 2)
            singleLine = !notes && (key in ONE_LINE_KEYS || hidden),
            minLines = if (notes) NOTES_LINES else 1,
            keyboardOptions = keyboardFor(key),
            hidden = hidden,
            secret = private,
            actions = if (private) {
                { VisibilityToggle(visible = shown, onToggle = { onToggleKey(key) }, enabled = enabled) }
            } else {
                null
            },
        )
    }

    // A field of the preset of the type "phone" - in the entry, as the file has it (pass-docs, Step 77)
    @Composable
    fun PresetPhone(key: String, region: String) {
        val private = state.protection.protects(key)
        val shown = key in state.shownKeys
        val digits = LocalPhoneNumbers.current.digits(content.value(key), region)
        PhoneField(
            prefix = phonePrefix(region),
            digits = digits,
            onDigitsChange = { onPhoneDigitsChange(PhoneTarget.Preset(key), it) },
            format = rememberPhoneFormat(region),
            onPrefixClick = { onChoosePhoneCountry(PhoneTarget.Preset(key)) },
            label = fieldLabel(key),
            modifier = Modifier.fillMaxWidth(),
            hint = phoneHint(region, digits),
            enabled = enabled,
            hidden = private && !shown,
            secret = private,
            actions = if (private) {
                { VisibilityToggle(visible = shown, onToggle = { onToggleKey(key) }, enabled = enabled) }
            } else {
                null
            },
        )
    }

    @Composable
    fun PresetInput(field: PresetField) {
        val inEntry = content.presetFields.first { it.key == field.key }
        val phoneRegion = state.regionOf(inEntry)
        if (FieldType.of(inEntry.type) == FieldType.PHONE && phoneRegion != null) {
            PresetPhone(field.key, phoneRegion)
        } else if (field.key == StandardPreset.PASSWORD) {
            PasswordField(
                value = content.value(field.key),
                onValueChange = { onValueChange(field.key, it) },
                label = fieldLabel(field.key),
                visible = state.passwordVisible,
                onToggleVisible = { onTogglePassword() },
                modifier = Modifier.fillMaxWidth(),
                required = field.required,
                error = if (state.isMissing(field.key)) missing else null,
                enabled = enabled,
                imeAction = ImeAction.Next,
                newPassword = true,
            )
        } else {
            PresetText(field)
        }
    }

    // The group and the preset in the card of the title, under it (pass-docs, Step 73, item 4)
    EntryTitleSection(
        enabled = enabled,
        title = { modifier -> PresetText(field = content.preset.fields.first(), modifier = modifier) },
    ) {
        Dropdown(
            selected = state.group,
            options = state.groups.map { it.option() },
            onSelect = { onGroupChosen(it) },
            label = stringResource(Res.string.entry_field_group),
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
        )

        // Presets switched off in the settings: no choice - a new entry stays with the standard preset, one saved
        // with its own (pass-docs, Step 62). A field as the group's, opening the dialog (Step 73, item 5)
        if (LocalAppDisplay.current.presets) {
            PickerField(
                value = presetName(content.preset),
                label = stringResource(Res.string.entry_preset),
                onClick = { onChoosePreset() },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
            )
        }
    }

    // The cards of the preset, as the preset has them
    content.preset.cards.forEach { card ->
        FieldSection {
            card.fields.forEach { PresetInput(it) }
        }
    }

    // A card of its own: the own fields - read or added here - and the button that adds one
    FieldSection {
        content.ownFields.forEachIndexed { index, field ->
            OwnFieldRow(
                field = field,
                private = state.protection.hides(field),
                shown = index in state.shownOwn,
                error = state.nameProblem(index)?.let { nameProblemText(it) },
                enabled = enabled,
                onValueChange = { onOwnValueChange(index, it) },
                onToggleShown = { onToggleOwn(index) },
                onEdit = { onEditField(index) },
                onDelete = { onDeleteField(index) },
                phoneRegion = state.regionOf(field),
                onPhoneDigitsChange = { onPhoneDigitsChange(PhoneTarget.Own(index), it) },
                onPhonePrefixClick = { onChoosePhoneCountry(PhoneTarget.Own(index)) },
            )
        }

        // Past the most fields a card takes there is no button; a field deleted brings it back
        if (state.canAddField) {
            SecondaryButton(
                text = stringResource(Res.string.entry_add_field),
                onClick = { onAddField() },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
            )
        }
    }
}

/**
 * The keyboard of a field of a preset: the standard ones each their own, a field the person named plain
 * text. Notes and a field the person named - both of many lines - leave "next" to the line break.
 */
private fun keyboardFor(key: String): KeyboardOptions = when (key) {
    StandardPreset.TITLE, StandardPreset.SERVICE ->
        KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next)
    StandardPreset.USER_NAME -> KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next)
    StandardPreset.URL ->
        KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false, imeAction = ImeAction.Next)
    StandardPreset.NOTES -> KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
    else -> KeyboardOptions.Default
}

/** The standard fields of a preset typed in one line; the password has a field of its own. */
private val ONE_LINE_KEYS = setOf(StandardPreset.TITLE, StandardPreset.USER_NAME, StandardPreset.SERVICE, StandardPreset.URL)

/** Room around the cards; at the bottom - for the fab, so it never covers the last field. */
private val CONTENT_PADDING = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp)

/** Notes start this tall and grow with the text. */
private const val NOTES_LINES = 3
