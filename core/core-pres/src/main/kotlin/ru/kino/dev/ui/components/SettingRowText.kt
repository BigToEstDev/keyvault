package ru.kino.dev.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The title of a row of settings and the line under it, when there is one - [SwitchRow], [ValueRow].
 *
 * The lines of one text sit closer than the styles of Material 3 have them - a title or a line that wraps, most of
 * them do in Russian, reads as one - and the line under the title is set apart from it instead (pass-docs, Step 59).
 */
@Composable
internal fun SettingRowText(title: String, text: String?, enabled: Boolean, modifier: Modifier = Modifier) {
    val alpha = if (enabled) 1f else DISABLED_ALPHA
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(TITLE_GAP)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = TITLE_LINE_HEIGHT),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
        )
        text?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = TEXT_LINE_HEIGHT),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            )
        }
    }
}

/** A disabled element of Material 3: its content in `onSurface` at 38%. Shared by the rows of settings. */
internal const val DISABLED_ALPHA = 0.38f

private val TITLE_LINE_HEIGHT = 18.sp
private val TEXT_LINE_HEIGHT = 16.sp

/** Between the title and the line under it. */
private val TITLE_GAP = 4.dp
