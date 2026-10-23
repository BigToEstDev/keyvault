package ru.kino.dev.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * A field to read, not to type into: its label, its value under it, buttons at the end - copy, the eye of a
 * secret. No outline - a field to read is no field to type into; the rows of a [FieldRowsSection] are parted by
 * lines (pass-docs, Step 73, item 3). The value wraps whole: nothing scrolls inside the row.
 *
 * @param hidden the value as dots - a secret
 * @param secret the value is a secret: shown in clear, colored by kind, as in a [TextField]
 * @param actions buttons at the end of the row
 */
@Composable
fun FieldRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    hidden: Boolean = false,
    secret: Boolean = false,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.rowRoom(actions != null),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = rememberSecretText(value, hidden, secret),
                style = MaterialTheme.typography.bodyLarge,
                // Dots are no text to read out
                modifier = if (hidden) Modifier.semantics { password() } else Modifier,
            )
        }
        actions?.let { Row(content = it) }
    }
}

/**
 * A [FieldRow] while its value is being read: a bar for the label, one for the value - of the heights of their
 * lines, in the room of the row, so nothing moves when the value comes (pass-docs, Step 73, item 6).
 */
@Composable
fun SkeletonFieldRow(brush: Brush, modifier: Modifier = Modifier) {
    Column(modifier = modifier.rowRoom(actions = false), verticalArrangement = Arrangement.Center) {
        SkeletonText(brush = brush, style = MaterialTheme.typography.bodySmall, widthFraction = LABEL_WIDTH)
        SkeletonText(brush = brush, style = MaterialTheme.typography.bodyLarge)
    }
}

/** The size and the padding of a row; the buttons have room of their own around the icon - the end is narrower. */
private fun Modifier.rowRoom(actions: Boolean): Modifier = this
    .fillMaxWidth()
    .heightIn(min = ROW_MIN_HEIGHT)
    .padding(start = 16.dp, end = if (actions) 4.dp else 16.dp, top = 8.dp, bottom = 8.dp)

/** Share of the width the bar of the label takes - a label is shorter than a value. */
private const val LABEL_WIDTH = 0.3f

/** Two lines of text, or the touch of a button, with room around. */
private val ROW_MIN_HEIGHT = 64.dp
