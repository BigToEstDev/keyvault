package ru.kino.dev.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.kino.dev.ui.theme.DISABLED_ALPHA

/**
 * A field to read, not to type into: its label, its value under it, buttons at the end - copy, the eye of a
 * secret. No outline - a field to read is no field to type into; the rows of a [FieldRowsSection] are parted by
 * lines (pass-docs, Step 73, item 3). The value wraps whole: nothing scrolls inside the row.
 *
 * @param hidden the value as dots - a secret
 * @param secret the value is a secret: shown in clear, colored by kind, as in a [TextField]
 * @param link the value is a link - in the primary color (pass-docs, Step 73, item 7)
 * @param actions buttons at the end of the row
 */
@Composable
fun FieldRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    hidden: Boolean = false,
    secret: Boolean = false,
    link: Boolean = false,
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
                color = if (link) MaterialTheme.colorScheme.primary else Color.Unspecified,
                // Dots are no text to read out
                modifier = if (hidden) Modifier.semantics { password() } else Modifier,
            )
        }
        actions?.let { Row(content = it) }
    }
}

/**
 * A file in a [FieldRowsSection] - an attachment of an entry (pass-docs, Step 103): the clip, the [name] - wrapped
 * whole - and the [size] under it, buttons at the end. In the room of a [FieldRow], so the cards look alike.
 *
 * Disabled - the text and the clip at 38%, as a disabled element of Material 3; the buttons disable themselves.
 */
@Composable
fun FileRow(
    name: String,
    size: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val alpha = if (enabled) 1f else DISABLED_ALPHA
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier.rowRoom(actions != null),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.AttachFile,
            contentDescription = null,
            tint = colors.onSurfaceVariant.copy(alpha = alpha),
            modifier = Modifier.padding(end = 12.dp).size(FILE_ICON_SIZE),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface.copy(alpha = alpha))
            Text(
                text = size,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant.copy(alpha = alpha),
            )
        }
        actions?.let { Row(verticalAlignment = Alignment.CenterVertically, content = it) }
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

private val FILE_ICON_SIZE = 20.dp

/** Share of the width the bar of the label takes - a label is shorter than a value. */
private const val LABEL_WIDTH = 0.3f

/** Two lines of text, or the touch of a button, with room around. */
private val ROW_MIN_HEIGHT = 64.dp
