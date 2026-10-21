package ru.kino.dev.vault.pres.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.kino.dev.core.pres.R
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.ui.components.CopyButton
import ru.kino.dev.ui.components.FieldSection
import ru.kino.dev.ui.components.PasswordField
import ru.kino.dev.ui.components.SensitiveClipboard
import ru.kino.dev.ui.components.TextField
import ru.kino.dev.ui.components.VisibilityToggle
import ru.kino.dev.ui.components.rememberSensitiveClipboard
import ru.kino.dev.ui.preset.fieldLabel
import ru.kino.dev.vault.entry.EntryContent
import ru.kino.dev.vault.entry.EntryField

/**
 * The fields of an entry that hold something, none taking input - its reading, or one of its old versions
 * (pass-docs, Step 51, item 6). An empty title reads "untitled".
 *
 * @param group where the entry is - a card of its own; null - no card: a version does not keep its group
 * @param copyable "copy" by the values; an old version has none - restore it to copy (owner, 17.10.26)
 * @param shownOwn indexes of the protected own fields shown in clear
 */
@Composable
internal fun EntryReadFields(
    content: EntryContent,
    passwordVisible: Boolean,
    shownOwn: Set<Int>,
    onTogglePassword: () -> Unit,
    onToggleOwn: (Int) -> Unit,
    group: GroupChoice? = null,
    copyable: Boolean = true,
) {
    val clipboard = rememberSensitiveClipboard().takeIf { copyable }

    EntryTitleSection { modifier ->
        val title = content.value(StandardPreset.TITLE)
        ReadField(
            label = fieldLabel(StandardPreset.TITLE),
            value = title.ifEmpty { stringResource(R.string.entry_untitled) },
            clipboard = clipboard.takeIf { title.isNotEmpty() },
            modifier = modifier,
        )
    }

    // Where the entry is: the name, the path down to it under it (pass-docs, Step 48)
    group?.let {
        FieldSection {
            TextField(
                value = it.title(),
                onValueChange = {},
                label = stringResource(R.string.entry_field_group),
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                hint = it.subtitle(),
            )
        }
    }

    // The cards of the preset, as the preset has them - each with the fields that hold something, if any
    content.preset.cards.forEach { card ->
        val shown = card.fields.map { it.key }.filter { content.value(it).isNotEmpty() }
        if (shown.isNotEmpty()) {
            FieldSection { PresetFields(content, shown, clipboard, passwordVisible, onTogglePassword) }
        }
    }

    val own = content.ownFields.withIndex().filter { it.value.value.isNotEmpty() }

    // A card of its own, as on the form
    if (own.isNotEmpty()) {
        FieldSection { OwnFields(own, clipboard, shownOwn, onToggleOwn) }
    }
}

@Composable
private fun PresetFields(
    content: EntryContent,
    keys: List<String>,
    clipboard: SensitiveClipboard?,
    passwordVisible: Boolean,
    onTogglePassword: () -> Unit,
) {
    keys.forEach { key ->
        if (key == StandardPreset.PASSWORD) {
            val label = fieldLabel(key)
            val value = content.value(key)
            PasswordField(
                value = value,
                onValueChange = {},
                label = label,
                visible = passwordVisible,
                onToggleVisible = onTogglePassword,
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                actions = clipboard?.let { { CopyButton(onCopy = { it.copy(label, value) }) } },
            )
        } else {
            ReadField(
                label = fieldLabel(key),
                value = content.value(key),
                clipboard = clipboard,
                multiLine = key == StandardPreset.NOTES,
            )
        }
    }
}

@Composable
private fun OwnFields(
    fields: List<IndexedValue<EntryField>>,
    clipboard: SensitiveClipboard?,
    shownOwn: Set<Int>,
    onToggleOwn: (Int) -> Unit,
) {
    fields.forEach { (index, field) ->
        val shown = index in shownOwn
        ReadField(
            label = field.key,
            value = field.value,
            clipboard = clipboard,
            hidden = field.protected && !shown,
            secret = field.protected,
            onToggleShown = if (field.protected) {
                { onToggleOwn(index) }
            } else {
                null
            },
            shown = shown,
        )
    }
}

/**
 * A field that only shows its value, with "copy" - when [clipboard] is given - and the eye of a protected one.
 *
 * @param secret a protected one: shown in clear, it is colored as a password is (pass-docs, Step 58)
 */
@Composable
private fun ReadField(
    label: String,
    value: String,
    clipboard: SensitiveClipboard?,
    modifier: Modifier = Modifier,
    multiLine: Boolean = false,
    hidden: Boolean = false,
    secret: Boolean = false,
    shown: Boolean = false,
    onToggleShown: (() -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = {},
        label = label,
        modifier = modifier.fillMaxWidth(),
        readOnly = true,
        hidden = hidden,
        secret = secret,
        singleLine = !multiLine,
        actions = if (clipboard == null && onToggleShown == null) {
            null
        } else {
            {
                // The eye first, then "copy" - as on the password
                onToggleShown?.let { VisibilityToggle(visible = shown, onToggle = it) }
                clipboard?.let { CopyButton(onCopy = { it.copy(label, value) }) }
            }
        },
    )
}
