package ru.kino.dev.vault.pres.entryedit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ru.kino.dev.core.pres.*
import ru.kino.dev.core.preset.EntryPreset
import ru.kino.dev.core.preset.PresetId
import ru.kino.dev.core.preset.PresetValidator
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.ui.components.SecondaryButton
import ru.kino.dev.ui.components.verticalFadingEdges
import ru.kino.dev.ui.preset.presetName
import ru.kino.dev.ui.res.stringResource

/**
 * The choice of presets (pass-docs, vault-our.md "Фича создание кастомных пресетов"): the standard one, then
 * [presets] - those of one's own, in the order they were made. Each is a card; the one of the form - [selected],
 * null for the standard one - is filled with the accent container; a tap picks one and closes. A long list
 * scrolls inside a height of its own, so the dialog does not spread over the screen. Under it - "create
 * preset", while one more can be made.
 *
 * A dialog of its own, not the filters' one: a list of cards under the title, not a form (pass-docs, Step 40,
 * fix 17).
 */
@Composable
internal fun PresetChoiceDialog(
    title: String,
    presets: List<EntryPreset>,
    selected: PresetId?,
    onChoose: (PresetId?) -> Unit,
    onCreate: () -> Unit,
    onClose: () -> Unit,
) {
    Dialog(onDismissRequest = onClose) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // The end is narrower - the "close" button sits there; the list keeps the start's width
                    .padding(start = 24.dp, end = 8.dp, top = 16.dp, bottom = 24.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.action_close))
                    }
                }
                val scroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp, end = 16.dp)
                        .heightIn(max = LIST_MAX_HEIGHT)
                        .verticalFadingEdges(scroll)
                        .verticalScroll(scroll),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    (listOf(StandardPreset.preset) + presets).forEach { preset ->
                        PresetCard(
                            name = presetName(preset),
                            selected = preset.id == selected,
                            onClick = { onChoose(preset.id) },
                        )
                    }
                }
                // No button past the most presets a person has (pass-docs, vault-our.md)
                if (PresetValidator.canAdd(presets)) {
                    SecondaryButton(
                        text = stringResource(Res.string.preset_create_title),
                        onClick = onCreate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, end = 16.dp),
                    )
                }
            }
        }
    }
}

/**
 * A preset of the list: a plain tile, no outline. The chosen one is told by its fill - the accent container -
 * and to TalkBack by the selected state, not by the color alone.
 */
@Composable
private fun PresetCard(name: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) colors.primaryContainer else colors.onSurface.copy(alpha = CARD_FILL_ALPHA),
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurface,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        )
    }
}

/** The list scrolls past this - ten presets fit the most a person has, on a short screen it scrolls. */
private val LIST_MAX_HEIGHT = 360.dp

/** A card not chosen - barely set off the dialog, in either theme. */
private const val CARD_FILL_ALPHA = 0.05f
