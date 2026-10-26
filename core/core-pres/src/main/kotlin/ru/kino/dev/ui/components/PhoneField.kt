package ru.kino.dev.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.R

/**
 * A field of a phone number (pass-docs, Step 77): the prefix of the country inside the border, before the number -
 * a tap on it asks for another country - and the number, grouped as the country writes it while it is typed.
 *
 * Without state of its own: it takes and gives the [digits] after the calling code, nothing else - the grouping is
 * drawn over them, the value kept in E.164 is made by the caller.
 *
 * @param prefix the calling code of the country, as it is shown - `+7`
 * @param format the [digits] grouped - see `PhoneNumbers.formatNational`
 * @param error marks the field as wrong - its name, say; [hint] - not an error: a number that is not of its country
 *   is kept all the same
 */
@Composable
fun PhoneField(
    prefix: String,
    digits: String,
    onDigitsChange: (String) -> Unit,
    format: (String) -> String,
    onPrefixClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    hint: String? = null,
    enabled: Boolean = true,
    hidden: Boolean = false,
    secret: Boolean = false,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val grouping = remember(format) { GroupingTransformation(format) }
    // The hint under the border in line with the field, not in from its edge as Material puts its supporting text
    Column(modifier = modifier) {
        TextField(
            value = digits,
            onValueChange = { typed -> onDigitsChange(typed.filter(Char::isDigit)) },
            label = label,
            modifier = Modifier.fillMaxWidth(),
            error = error,
            enabled = enabled,
            hidden = hidden,
            secret = secret,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            actions = actions,
            transformation = grouping,
            leading = {
                TextButton(
                    onClick = onPrefixClick,
                    enabled = enabled,
                    modifier = Modifier.padding(start = 4.dp),
                ) {
                    Text(prefix)
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(R.string.phone_prefix_choose))
                }
            },
        )
        hint?.let { HintText(it, enabled = enabled, indented = false) }
    }
}

/**
 * The digits drawn grouped by [format]; the cursor moves over the digits only, the spaces and dashes between them
 * skipped. A grouping that does not keep every digit in its order is not drawn - the digits are, as they are.
 */
internal class GroupingTransformation(private val format: (String) -> String) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val shown = format(digits)
        // Where each digit lands in what is shown
        val places = shown.indices.filter { shown[it].isDigit() }
        if (shown.filter(Char::isDigit) != digits) return TransformedText(text, OffsetMapping.Identity)

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                if (offset == 0) 0 else places[(offset - 1).coerceAtMost(places.lastIndex)] + 1

            override fun transformedToOriginal(offset: Int): Int = places.count { it < offset }
        }
        return TransformedText(AnnotatedString(shown), mapping)
    }

    override fun equals(other: Any?): Boolean = other is GroupingTransformation && other.format == format

    override fun hashCode(): Int = format.hashCode()
}
