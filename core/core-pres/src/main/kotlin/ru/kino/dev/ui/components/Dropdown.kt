package ru.kino.dev.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * One choice of a [Dropdown]: [title] is what the field shows once it is chosen; [subtitle], when there is
 * one, is a line under the title in the list - where the choice lies, say.
 */
data class DropdownOption<T>(val value: T, val title: String, val subtitle: String? = null)

/**
 * A field that takes one of [options], picked from a list that drops from it - the look of the other
 * fields, outlined, with the label on its border. The field shows the title of the [selected] one only; the
 * list shows each with its subtitle under it, cut at the start when long: the end of a path is what tells
 * two choices apart.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Dropdown(
    selected: T?,
    options: List<DropdownOption<T>>,
    onSelect: (T) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val open = expanded && enabled

    ExposedDropdownMenuBox(
        expanded = open,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = options.firstOrNull { it.value == selected }?.title.orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, option ->
                // A line between the choices: each is two lines of text, and they run together without it
                if (index > 0) HorizontalDivider()
                DropdownMenuItem(
                    text = { OptionText(option) },
                    onClick = {
                        expanded = false
                        onSelect(option.value)
                    },
                    contentPadding = ITEM_PADDING,
                )
            }
        }
    }
}

@Composable
private fun OptionText(option: DropdownOption<*>) {
    Column {
        Text(text = option.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        option.subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.StartEllipsis,
            )
        }
    }
}

/**
 * Around the text of a choice: the side padding of Material 3 (`ExposedDropdownMenuDefaults.ItemContentPadding`),
 * and room above and below, which it leaves at none - two lines of a choice sat close to the divider.
 */
private val ITEM_PADDING = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
