package ru.kino.dev.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import ru.kino.dev.core.pres.R

/**
 * A password field - a [TextField] hidden behind dots until [visible], without state of its own.
 *
 * @param onToggleVisible null hides the toggle - for a field that follows the toggle of another one
 * @param actions buttons after the toggle - copying the value, on a screen that reads
 * @see TextField for the rest
 */
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisible: (() -> Unit)?,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    error: String? = null,
    hint: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        required = required,
        error = error,
        hint = hint,
        enabled = enabled,
        readOnly = readOnly,
        hidden = !visible,
        secret = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            autoCorrectEnabled = false,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        actions = if (actions == null && onToggleVisible == null) {
            null
        } else {
            {
                onToggleVisible?.let { VisibilityToggle(visible = visible, onToggle = it, enabled = enabled) }
                actions?.invoke(this)
            }
        },
    )
}

/** The eye that shows or hides a secret. */
@Composable
fun VisibilityToggle(visible: Boolean, onToggle: () -> Unit, enabled: Boolean = true) {
    IconButton(onClick = onToggle, enabled = enabled) {
        Icon(
            imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
            contentDescription = stringResource(if (visible) R.string.password_hide else R.string.password_show),
        )
    }
}
