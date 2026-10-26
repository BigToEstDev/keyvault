package ru.kino.dev.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.FieldType
import ru.kino.dev.core.pres.R

/**
 * A field the person names - of an entry or of a preset, added or edited: its name and, under it, whether its
 * value is protected - hidden, with an eye, and its type (pass-docs, Step 75). One dialog for both screens and
 * both ways in.
 *
 * Without state of its own, as [TextInputDialog]: the name, the switch, the type and the error come from the caller.
 *
 * @param type the type shown - one this app does not know is shown as text, see [FieldType.of]
 */
@Composable
fun FieldDialog(
    title: String,
    name: String,
    onNameChange: (String) -> Unit,
    protected: Boolean,
    onProtectedChange: (Boolean) -> Unit,
    type: FieldType,
    onTypeChange: (FieldType) -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    error: String? = null,
) {
    TextInputDialog(
        title = title,
        value = name,
        onValueChange = onNameChange,
        label = stringResource(R.string.entry_field_name),
        confirmText = confirmText,
        dismissText = stringResource(R.string.action_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        error = error,
        below = {
            Column {
                SwitchRow(
                    title = stringResource(R.string.field_protected),
                    checked = protected,
                    onCheckedChange = onProtectedChange,
                    modifier = Modifier.padding(top = 8.dp),
                    // The dialog pads its text already: the switch lines up with the field
                    contentPadding = PaddingValues(vertical = 12.dp),
                )
                Dropdown(
                    selected = type,
                    options = FieldType.entries.map { DropdownOption(it, fieldTypeTitle(it)) },
                    onSelect = onTypeChange,
                    label = stringResource(R.string.field_type),
                    modifier = Modifier.fillMaxWidth(),
                )
                // In line with the field and the switch above it, as the rest of the dialog
                HintText(stringResource(R.string.field_type_hint), indented = false)
            }
        },
    )
}

/** What a type of a field is called on the screens. */
@Composable
fun fieldTypeTitle(type: FieldType): String = when (type) {
    FieldType.TEXT -> stringResource(R.string.field_type_text)
}
