package ru.kino.dev.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/**
 * A field that shows the [value] chosen and, on a tap, opens the caller's own choosing - a dialog, where a
 * [Dropdown] drops a list from itself. The look of a [Dropdown]: outlined, the label on the border, the arrow at
 * the end - choices on a form look alike (pass-docs, Step 73, item 5).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerField(
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = false) },
            // What it says, the tap over it says
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics {},
        )
        // The field takes no input: the tap over its box is the choice. The box is under the room of the label
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = LABEL_ROOM)
                .clip(OutlinedTextFieldDefaults.shape)
                .semantics {
                    contentDescription = label
                    stateDescription = value
                }
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        )
    }
}

/** The room an outlined field keeps over its box for the label (`OutlinedTextField`, Material 3). */
private val LABEL_ROOM = 8.dp
