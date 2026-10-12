package ru.kino.dev.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import ru.kino.dev.core.pres.R

/**
 * A password field without state of its own: what it shows, whether the password is visible and the error
 * all come from the caller.
 *
 * It does not limit the length itself - the processor decides what a value too long means, keeps the old
 * one and passes the reason as [error].
 *
 * @param onToggleVisible null hides the toggle - for a field that follows the toggle of another one
 * @param error marks the field as wrong and puts a "!" in it; the text is a bubble under the field, over the
 * layout - see [FieldErrorIcon]
 * @param hint shown under the field, error or not
 */
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisible: (() -> Unit)?,
    modifier: Modifier = Modifier,
    error: String? = null,
    hint: String? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
) {
    val errorState = rememberFieldErrorState(error)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.onFocusChanged { errorState.onFocusChanged(it.isFocused, enabled) },
        enabled = enabled,
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        supportingText = hint?.let { { Text(it) } },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            autoCorrectEnabled = false,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        trailingIcon = if (error == null && onToggleVisible == null) {
            null
        } else {
            {
                Row {
                    error?.let { FieldErrorIcon(error = it, state = errorState) }
                    onToggleVisible?.let { toggle ->
                        IconButton(onClick = toggle, enabled = enabled) {
                            Icon(
                                imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = stringResource(
                                    if (visible) R.string.password_hide else R.string.password_show,
                                ),
                            )
                        }
                    }
                }
            }
        },
    )
}
