package ru.kino.dev.settings.pres.entries.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * "The phone prefix by default" (pass-docs, Step 77): the title, and under it - the whole width, not beside it as
 * `ValueRow` puts a value - the prefix and the country in the accent color. A long title and a long name of a country
 * do not fit one line together; a name too long for its own ends in "…", the prefix before it stays whole.
 */
@Composable
internal fun PhonePrefixRow(title: String, value: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = TITLE_LINE_HEIGHT),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = VALUE_GAP),
        )
    }
}

// As the title of a row of the settings
private val TITLE_LINE_HEIGHT = 18.sp

private val VALUE_GAP = 4.dp
