package ru.kino.dev.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.R
import ru.kino.dev.ui.theme.LocalAppColors

/**
 * A password field - a [TextField] hidden behind dots until [visible], without state of its own.
 *
 * @param onToggleVisible null hides the toggle - for a field that follows the toggle of another one
 * @param actions buttons after the toggle - copying the value, on a screen that reads
 * @param newPassword a password being made, not one typed to get in: its strength on the border of the field, when
 * the setting is on (pass-docs, Step 74)
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
    newPassword: Boolean = false,
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
        strength = newPassword,
    )
}

/**
 * The strength of a password on the bottom edge of the border of its field: a thin line, a third of the width for a
 * weak one, two thirds for a medium one, whole for a strong one (pass-docs, Step 74). Laid over the field, clipped to
 * its corners; [strength] null - none. It grows and changes color smoothly as the password is typed.
 */
@Composable
internal fun StrengthBar(strength: PasswordStrength?, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val fraction by animateFloatAsState(
        targetValue = when (strength) {
            null -> 0f
            PasswordStrength.Weak -> WEAK_SHARE
            PasswordStrength.Medium -> MEDIUM_SHARE
            PasswordStrength.Strong -> 1f
        },
        label = "strength-width",
    )
    val color by animateColorAsState(
        targetValue = when (strength) {
            PasswordStrength.Weak, null -> MaterialTheme.colorScheme.error
            PasswordStrength.Medium -> colors.strengthMedium
            PasswordStrength.Strong -> colors.strengthStrong
        },
        label = "strength-color",
    )
    Box(modifier = modifier.clip(OutlinedTextFieldDefaults.shape), contentAlignment = Alignment.BottomStart) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(BAR_HEIGHT)
                .background(color),
        )
    }
}

private const val WEAK_SHARE = 1f / 3
private const val MEDIUM_SHARE = 2f / 3

/** Thin - over the line of the border, a little thicker than it. */
private val BAR_HEIGHT = 3.dp

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
