package ru.kino.dev.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.R

/**
 * A field the person names - of an entry or of a preset, added or edited: its name and, under it, whether its
 * value is protected - hidden, with an eye (pass-docs, Step 75). One dialog for both screens and both ways in.
 *
 * Without state of its own, as [TextInputDialog]: the name, the switch and the error come from the caller.
 */
@Composable
fun FieldDialog(
    title: String,
    name: String,
    onNameChange: (String) -> Unit,
    protected: Boolean,
    onProtectedChange: (Boolean) -> Unit,
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
            SwitchRow(
                title = stringResource(R.string.field_protected),
                checked = protected,
                onCheckedChange = onProtectedChange,
                modifier = Modifier.padding(top = 8.dp),
                // The dialog pads its text already: the switch lines up with the field
                contentPadding = PaddingValues(vertical = 12.dp),
            )
        },
    )
}
