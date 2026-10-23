package ru.kino.dev.vault.pres.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.FieldProtection
import ru.kino.dev.core.pres.R
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.ui.components.CopyButton
import ru.kino.dev.ui.components.FieldRow
import ru.kino.dev.ui.components.FieldRowsSection
import ru.kino.dev.ui.components.FieldSection
import ru.kino.dev.ui.components.SensitiveClipboard
import ru.kino.dev.ui.components.VisibilityToggle
import ru.kino.dev.ui.components.rememberSecretText
import ru.kino.dev.ui.components.rememberSensitiveClipboard
import ru.kino.dev.ui.preset.fieldLabel
import ru.kino.dev.vault.entry.EntryContent
import ru.kino.dev.vault.entry.EntryField

/**
 * The fields of an entry that hold something, none taking input - its reading, or one of its old versions
 * (pass-docs, Step 51, item 6). An empty title reads "untitled".
 *
 * Rows of a label and a value, not fields: a field to read is no field to type into (pass-docs, Step 73, item 3).
 *
 * @param group where the entry is - a chip under the title; null - none: a version does not keep its group
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

    TitleSection(content.value(StandardPreset.TITLE), clipboard, privacy)

    // Where the entry is: the path down to it, the group at its end (pass-docs, Step 48, Step 73 item 3)
    group?.let { GroupChip(path = it.subtitle() ?: it.title()) }

    // The cards of the preset, as the preset has them - each with the fields that hold something, if any
    content.preset.cards.forEach { card ->
        val shown = card.fields.map { it.key }.filter { content.value(it).isNotEmpty() }
        if (shown.isNotEmpty()) {
            FieldRowsSection(
                rows = shown.map { key ->
                    { PresetRow(content, key, clipboard, passwordVisible, onTogglePassword, privacy) }
                },
            )
        }
    }

    val own = content.ownFields.withIndex().filter { it.value.value.isNotEmpty() }

    // A card of its own, as on the form
    if (own.isNotEmpty()) {
        FieldRowsSection(
            rows = own.map { (index, field) ->
                { OwnRow(index, field, clipboard, shownOwn, onToggleOwn, protection) }
            },
        )
    }
}

/**
 * The first card: the icon and the title beside it, large - the name of the entry, not a field with a label.
 * A private title keeps its eye, a filled one - "copy".
 */
@Composable
private fun TitleSection(title: String, clipboard: SensitiveClipboard?, privacy: Privacy) {
    // "Untitled" is no value to hide or to copy
    val private = title.isNotEmpty() && privacy.protection.protects(StandardPreset.TITLE)
    val shown = StandardPreset.TITLE in privacy.shownKeys
    val hidden = private && !shown
    val label = fieldLabel(StandardPreset.TITLE)
    FieldSection {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EntryIconFrame()
            Text(
                text = rememberSecretText(title.ifEmpty { stringResource(R.string.entry_untitled) }, hidden, private),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
                    .then(if (hidden) Modifier.semantics { password() } else Modifier),
            )
            if (private) VisibilityToggle(visible = shown, onToggle = { privacy.onToggleKey(StandardPreset.TITLE) })
            if (title.isNotEmpty()) clipboard?.let { CopyButton(onCopy = { it.copy(label, title) }) }
        }
    }
}

/** The group of the entry: a chip outlined, the folder and the path; a long path is cut at the start. */
@Composable
private fun GroupChip(path: String) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        // Over the backdrop, as the cards are: only the outline sets it off
        color = Color.Transparent,
        border = BorderStroke(1.dp, colors.outline),
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Folder,
                contentDescription = stringResource(R.string.entry_field_group),
                tint = colors.primary,
                modifier = Modifier.size(CHIP_ICON_SIZE),
            )
            Text(
                text = path,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.StartEllipsis,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun PresetRow(
    content: EntryContent,
    key: String,
    clipboard: SensitiveClipboard?,
    passwordVisible: Boolean,
    onTogglePassword: () -> Unit,
    privacy: Privacy,
) {
    if (key == StandardPreset.PASSWORD) {
        ReadRow(
            label = fieldLabel(key),
            value = content.value(key),
            clipboard = clipboard,
            hidden = !passwordVisible,
            secret = true,
            shown = passwordVisible,
            onToggleShown = onTogglePassword,
        )
    } else {
        val private = privacy.protection.protects(key)
        val shown = key in privacy.shownKeys
        ReadRow(
            label = fieldLabel(key),
            value = content.value(key),
            clipboard = clipboard,
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

@Composable
private fun OwnRow(
    index: Int,
    field: EntryField,
    clipboard: SensitiveClipboard?,
    shownOwn: Set<Int>,
    onToggleOwn: (Int) -> Unit,
    protection: FieldProtection,
) {
    val shown = index in shownOwn
    val private = protection.hides(field)
    ReadRow(
        label = field.key,
        value = field.value,
        clipboard = clipboard,
        hidden = private && !shown,
        secret = private,
        shown = shown,
        onToggleShown = if (private) {
            { onToggleOwn(index) }
        } else {
            null
        },
    )
}

/** The fields of the preset private by the database, and those of them shown in clear. */
private class Privacy(
    val protection: FieldProtection,
    val shownKeys: Set<String>,
    val onToggleKey: (String) -> Unit,
)

/**
 * A row that shows a value, with "copy" - when [clipboard] is given - and the eye of a protected one.
 *
 * @param secret a protected one: shown in clear, it is colored as a password is (pass-docs, Step 58)
 */
@Composable
private fun ReadRow(
    label: String,
    value: String,
    clipboard: SensitiveClipboard?,
    hidden: Boolean,
    secret: Boolean,
    shown: Boolean,
    onToggleShown: (() -> Unit)?,
) {
    FieldRow(
        label = label,
        value = value,
        hidden = hidden,
        secret = secret,
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

private val CHIP_ICON_SIZE = 18.dp
