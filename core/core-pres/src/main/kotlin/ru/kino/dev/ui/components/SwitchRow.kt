package ru.kino.dev.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * A setting that is on or off: a title, a line under it when there is one, and a switch at the end; the whole
 * row taps. Padded itself, so the tap fills the card it is put in; [contentPadding] - for a place already padded,
 * such as a dialog.
 *
 * Disabled - the text in `onSurface` at 38%, as a disabled element of Material 3.
 */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingRowText(title, text, enabled, modifier = Modifier.weight(1f).padding(end = 16.dp))
        // The row takes the tap and says what it is; the switch only draws it
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
