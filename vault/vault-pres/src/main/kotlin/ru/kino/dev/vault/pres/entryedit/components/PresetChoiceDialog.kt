package ru.kino.dev.vault.pres.entryedit.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.preset.EntryPreset
import ru.kino.dev.core.preset.PresetId
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.ui.components.verticalFadingEdges
import ru.kino.dev.vault.pres.common.ClosableDialog
import ru.kino.dev.vault.pres.common.presetName

/**
 * The choice of presets (pass-docs, vault-our.md "Фича создание кастомных пресетов"): the standard one, then
 * [presets] - those of one's own, in the order they were made. The one of the form - [selected], null for the
 * standard one - is marked; a tap picks one and closes. A long list scrolls inside a height of its own, so the
 * dialog does not spread over the screen.
 */
@Composable
internal fun PresetChoiceDialog(
    title: String,
    presets: List<EntryPreset>,
    selected: PresetId?,
    onChoose: (PresetId?) -> Unit,
    onClose: () -> Unit,
) {
    ClosableDialog(title = title, onClose = onClose) {
        val scroll = rememberScrollState()
        Column(
            modifier = Modifier
                .heightIn(max = LIST_MAX_HEIGHT)
                .verticalFadingEdges(scroll)
                .verticalScroll(scroll),
        ) {
            (listOf(StandardPreset.preset) + presets).forEachIndexed { index, preset ->
                if (index > 0) HorizontalDivider()
                PresetRow(name = presetName(preset), selected = preset.id == selected, onClick = { onChoose(preset.id) })
            }
        }
    }
}

@Composable
private fun PresetRow(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            // The end is the dialog's own, narrower - the "close" button sits there
            .padding(start = 0.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            // Marked, not announced: the name in the primary color says it too
            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** The list scrolls past this - ten presets fit the most a person has, on a short screen it scrolls. */
private val LIST_MAX_HEIGHT = 360.dp
