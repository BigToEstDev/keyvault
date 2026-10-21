package ru.kino.dev.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.R

/*
 * What the forms of an entry and of a preset share (pass-docs, Step 62: the form of a preset is in `settings`):
 * the card of fields, the pen of a field the person named, the check that saves.
 */

/**
 * A section of fields one under another, in a card outlined in the accent of the palette over a fill of the
 * palette - lighter than the backdrop in either theme. The default fill of a card is not of our palette and looks
 * muddy over the gradient (pass-docs, Step 40, fix 3); a faint text color in the dark theme was all but gone
 * (pass-docs, Step 56, item 2).
 */
@Composable
fun FieldSection(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    // Over a dark backdrop - the variant of the surface, whole; over a light one - the surface, nearly whole
    val fill = if (colors.background.luminance() < HALF) {
        colors.surfaceVariant
    } else {
        colors.surface.copy(alpha = LIGHT_FILL_ALPHA)
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = fill),
        border = BorderStroke(1.dp, colors.tertiary),
    ) {
        Column(
            modifier = Modifier.padding(SECTION_PADDING),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/**
 * The pen by a field the person named - outside the field, as the filter button sits by the search: it opens a
 * menu, rename and delete (pass-docs, vault-our.md "Свое поле").
 */
@Composable
fun FieldMenuButton(enabled: Boolean, onRename: () -> Unit, onDelete: () -> Unit) {
    MenuButton(
        icon = Icons.Filled.Edit,
        contentDescription = stringResource(R.string.entry_field_menu),
        items = listOf(renameMenuItem(onRename), deleteMenuItem(onDelete)),
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
            Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.entry_save))
        }
    }
}

/** Room inside a section; what stands under the sections in line with their content takes it too. */
val SECTION_PADDING = 16.dp

/** The fill of a section over the light theme; the line between a dark backdrop and a light one. */
private const val LIGHT_FILL_ALPHA = 0.85f
private const val HALF = 0.5f
