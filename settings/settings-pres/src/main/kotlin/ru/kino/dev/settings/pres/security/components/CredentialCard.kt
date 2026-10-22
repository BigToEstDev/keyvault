package ru.kino.dev.settings.pres.security.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kino.dev.ui.components.ValueRow

/**
 * A credential of the database in a card of its own (pass-docs, Step 67): the row - its title and value, the tap -
 * then, under a divider, what changing it means. [footer] - under the row, the progress while it is written;
 * [overlay] - in the box of the row, a menu anchored to it.
 */
@Composable
internal fun CredentialCard(
    title: String,
    value: String,
    hint: String,
    onClick: () -> Unit,
    enabled: Boolean,
    footer: @Composable () -> Unit = {},
    overlay: @Composable () -> Unit = {},
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            Box {
                ValueRow(title = title, value = value, onClick = onClick, enabled = enabled)
                overlay()
            }
            footer()
            HorizontalDivider()
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}
