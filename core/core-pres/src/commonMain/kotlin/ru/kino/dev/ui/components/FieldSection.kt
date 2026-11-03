package ru.kino.dev.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.res.stringResource

/*
 * What the forms of an entry and of a preset share (pass-docs, Step 62: the form of a preset is in `settings`):
 * the card of fields, the pen of a field the person named, the check that saves.
 */

/**
 * A section of fields one under another, in a card set off the backdrop by its fill alone - the high container of
 * the palette, no outline: the fields have outlines of their own, and a frame round frames was noise
 * (pass-docs, Step 73, item 2).
 */
@Composable
fun FieldSection(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    SectionCard(modifier) {
        Column(
            modifier = Modifier.padding(SECTION_PADDING),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/**
 * A section of [FieldRow]s - a card as a [FieldSection], to read: the rows run to its edges, a thin line between
 * each two (pass-docs, Step 73, item 3).
 */
@Composable
fun FieldRowsSection(rows: List<@Composable () -> Unit>, modifier: Modifier = Modifier) {
    val divider = MaterialTheme.colorScheme.outline.copy(alpha = DIVIDER_ALPHA)
    SectionCard(modifier) {
        rows.forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider(color = divider)
            row()
        }
    }
}

/**
 * On the backdrop - the high container; on a card of a container, itself the high container ([LocalScreenBackdrop]
 * false) - the highest one, as the cards of a list are (`Card` of Material 3 by default): on the card the list and the
 * entry look alike (pass-docs, Step 99, item 3).
 */
@Composable
private fun SectionCard(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = SECTION_SHAPE,
        colors = CardDefaults.cardColors(
            containerColor = if (LocalScreenBackdrop.current) colors.surfaceContainerHigh else colors.surfaceContainerHighest,
        ),
        content = content,
    )
}

/**
 * The pen by a field the person named - outside the field, as the filter button sits by the search: it opens a
 * menu, edit - the name and the protection - and delete (pass-docs, vault-our.md "Свое поле", Step 75).
 */
@Composable
fun FieldMenuButton(enabled: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    MenuButton(
        icon = Icons.Filled.Edit,
        contentDescription = stringResource(Res.string.entry_field_menu),
        items = listOf(editMenuItem(onEdit), deleteMenuItem(onDelete)),
        enabled = enabled,
    )
}

/** The check that saves; while the change is made and written, it waits and takes no taps. */
@Composable
fun SaveFab(saving: Boolean, onSave: () -> Unit) {
    FloatingActionButton(onClick = { if (!saving) onSave() }) {
        if (saving) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current,
            )
        } else {
            Icon(Icons.Filled.Check, contentDescription = stringResource(Res.string.entry_save))
        }
    }
}

/** Room inside a section; what stands under the sections in line with their content takes it too. */
val SECTION_PADDING = 16.dp

/** Rounder than a plain card of the theme - a section is wide and tall. */
private val SECTION_SHAPE = RoundedCornerShape(16.dp)

/** The line between rows - faint: it parts them, the fill of the card groups them. */
private const val DIVIDER_ALPHA = 0.25f
