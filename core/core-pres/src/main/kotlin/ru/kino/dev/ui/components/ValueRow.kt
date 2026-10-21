package ru.kino.dev.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * A setting with a value: a title, a line under it when there is one, the value at the end; a tap - to change
 * it, in a dialog of the caller. Padded itself, as [SwitchRow], so the tap fills the card it is put in. Without
 * [onClick] - a value to read only: no tap, no ripple (pass-docs, Step 63). With [icon] - it at the very end, in
 * the colour of the title, as the icons of the cards of settings: a pen says the text is changed by a tap.
 *
 * Disabled - the text in `onSurface` at 38%, as a disabled element of Material 3.
 */
@Composable
fun ValueRow(
    title: String,
    value: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    text: String? = null,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val alpha = if (enabled) 1f else DISABLED_ALPHA
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingRowText(title, text, enabled, modifier = Modifier.weight(1f).padding(end = 16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
        )
        // The row says what it is and takes the tap; the icon only draws it
        icon?.let { Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)) }
    }
}
