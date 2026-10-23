package ru.kino.dev.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import ru.kino.dev.ui.theme.AppDisplay
import ru.kino.dev.ui.theme.LocalAppColors
import ru.kino.dev.ui.theme.LocalAppDisplay

/**
 * The text field of the app, without state of its own: the value, the error and whether it is hidden all
 * come from the caller. Every other field - a password, the field of a dialog - is built on it.
 *
 * It does not limit the length itself - the processor decides what a value too long means.
 *
 * @param required puts a star after the label: the field must be filled
 * @param error marks the field as wrong and puts a "!" in it; the text is a bubble under the field, over the
 * layout - see [FieldErrorIcon]
 * @param hint shown under the field, error or not
 * @param readOnly shows the value without letting it change - a screen that reads, not edits
 * @param hidden shows the value as dots - a secret
 * @param secret the value is a secret - a password, a protected field: shown in clear, its letters, digits and
 * symbols each take their own color when [AppDisplay.passwordColoring] is on (pass-docs, Step 58); the dots
 * of a hidden one are not colored
 * @param singleLine false lets the text wrap and the field grow with it - notes
 * @param minLines how tall a multi-line field starts
 * @param actions buttons at the end of the field, after the "!" of the error
 */
@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    error: String? = null,
    hint: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    hidden: Boolean = false,
    secret: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val errorState = rememberFieldErrorState(error)
    val transformation = secretTransformation(hidden, secret)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .onFocusChanged { errorState.onFocusChanged(it.isFocused, enabled) }
            // A field only to read takes no focus: focus would light it up as a field to type into, and there
            // is nothing to select it for - its value is copied by its button (Step 40, fix 16)
            .focusProperties { canFocus = !readOnly },
        enabled = enabled,
        readOnly = readOnly,
        label = { FieldLabel(label, required) },
        singleLine = singleLine,
        minLines = minLines,
        isError = error != null,
        supportingText = hint?.let { { Text(it) } },
        visualTransformation = transformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        trailingIcon = if (error == null && actions == null) {
            null
        } else {
            {
                Row {
                    error?.let { FieldErrorIcon(error = it, state = errorState) }
                    actions?.invoke(this)
                }
            }
        },
    )
}

/**
 * How a value is drawn: as dots while [hidden]; a [secret] in clear - its digits and symbols in their colors when
 * [AppDisplay.passwordColoring] is on (pass-docs, Step 58); anything else as it is.
 */
@Composable
private fun secretTransformation(hidden: Boolean, secret: Boolean): VisualTransformation {
    val colors = LocalAppColors.current
    return when {
        hidden -> PasswordVisualTransformation()
        secret && LocalAppDisplay.current.passwordColoring ->
            remember(colors) { SecretColorTransformation(colors.passwordDigit, colors.passwordSymbol) }
        else -> VisualTransformation.None
    }
}

/**
 * [value] drawn as a [TextField] draws it - dots while [hidden], a [secret] colored by kind - for a text that only
 * shows it (pass-docs, Step 73, item 3).
 */
@Composable
fun rememberSecretText(value: String, hidden: Boolean, secret: Boolean): AnnotatedString {
    val transformation = secretTransformation(hidden, secret)
    return remember(value, transformation) { transformation.filter(AnnotatedString(value)).text }
}

/** The label of a field; a required one ends with a star in the color of errors. */
@Composable
private fun FieldLabel(label: String, required: Boolean) {
    val starColor = MaterialTheme.colorScheme.error
    Text(
        buildAnnotatedString {
            append(label)
            if (required) {
                withStyle(SpanStyle(color = starColor)) { append(REQUIRED_MARK) }
            }
        },
    )
}

private const val REQUIRED_MARK = " *"
