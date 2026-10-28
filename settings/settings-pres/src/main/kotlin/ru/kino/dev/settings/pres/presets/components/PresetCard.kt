package ru.kino.dev.settings.pres.presets.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.components.MenuButton
import ru.kino.dev.ui.components.MenuItem
import ru.kino.dev.ui.res.stringResource

/**
 * A preset in the list: its [name], and the pen with [menu] under it - "Edit", "Delete". The standard preset has no
 * menu, so no pen: it is not one's own to change (pass-docs, Step 62).
 */
@Composable
internal fun PresetCard(name: String, menu: List<MenuItem>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // As tall as the rows of settings, with the pen or without
                .heightIn(min = 56.dp)
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (menu.isNotEmpty()) {
                MenuButton(
                    icon = Icons.Filled.Edit,
                    contentDescription = stringResource(Res.string.preset_menu),
                    items = menu,
                )
            }
        }
    }
}
