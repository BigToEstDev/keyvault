package ru.kino.dev.vault.pres.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.kino.dev.core.FieldProtection
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
 * @param protection the fields private by the database - hidden, with the eye, whatever the preset (Step 65)
 * @param shownKeys keys of the fields of the preset private by the database shown in clear
 */
@Composable
internal fun EntryReadFields(
    content: EntryContent,
    passwordVisible: Boolean,
    shownOwn: Set<Int>,
    onTogglePassword: () -> Unit,
    onToggleOwn: (Int) -> Unit,
    protection: FieldProtection,
    shownKeys: Set<String>,
    onToggleKey: (String) -> Unit,
    group: GroupChoice? = null,
    copyable: Boolean = true,
) {
    val clipboard = rememberSensitiveClipboard().takeIf { copyable }
    val privacy = Privacy(protection, shownKeys, onToggleKey)

    EntryTitleSection { modifier ->
        val title = content.value(StandardPreset.TITLE)
        // "Untitled" is no value to hide
        val private = title.isNotEmpty() && protection.protects(StandardPreset.TITLE)
        ReadField(
            label = fieldLabel(StandardPreset.TITLE),
            value = title.ifEmpty { stringResource(R.string.entry_untitled) },
            clipboard = clipboard.takeIf { title.isNotEmpty() },
            modifier = modifier,
            hidden = private && StandardPreset.TITLE !in shownKeys,
            secret = private,
            shown = StandardPreset.TITLE in shownKeys,
            onToggleShown = if (private) {
                { onToggleKey(StandardPreset.TITLE) }
            } else {
                null
            },
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
            FieldSection { PresetFields(content, shown, clipboard, passwordVisible, onTogglePassword, privacy) }
        }
    }

    val own = content.ownFields.withIndex().filter { it.value.value.isNotEmpty() }

    // A card of its own, as on the form
    if (own.isNotEmpty()) {
        FieldSection { OwnFields(own, clipboard, shownOwn, onToggleOwn, protection) }
    }
}

@Composable
private fun PresetFields(
    content: EntryContent,
    keys: List<String>,
    clipboard: SensitiveClipboard?,
    passwordVisible: Boolean,
    onTogglePassword: () -> Unit,
    privacy: Privacy,
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
            val private = privacy.protection.protects(key)
            val shown = key in privacy.shownKeys
            ReadField(
                label = fieldLabel(key),
                value = content.value(key),
                clipboard = clipboard,
                multiLine = key == StandardPreset.NOTES,
                hidden = private && !shown,
                secret = private,
                shown = shown,
                onToggleShown = if (private) {
                    { privacy.onToggleKey(key) }
                } else {
                    null
                },
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
    protection: FieldProtection,
) {
    fields.forEach { (index, field) ->
        val shown = index in shownOwn
        val private = protection.hides(field)
        ReadField(
            label = field.key,
            value = field.value,
            clipboard = clipboard,
            hidden = private && !shown,
            secret = private,
            onToggleShown = if (private) {
                { onToggleOwn(index) }
            } else {
                null
            },
            shown = shown,
        )
    }
}

/** The fields of the preset private by the database, and those of them shown in clear. */
private class Privacy(
    val protection: FieldProtection,
    val shownKeys: Set<String>,
    val onToggleKey: (String) -> Unit,
)

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
