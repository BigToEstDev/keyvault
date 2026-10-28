package ru.kino.dev.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/** What a character of a secret is, for its color (pass-docs, Step 58). */
internal enum class SecretCharKind {
    /** Any letter, Cyrillic too - the color of the text of the field. */
    Letter,
    Digit,

    /** Everything else: punctuation, signs, a space. */
    Symbol,
}

/** A stretch of a secret, [start] included and [end] not, of one [kind]. */
internal data class SecretRun(val start: Int, val end: Int, val kind: SecretCharKind)

/** [text] cut into stretches of one kind each - next to each other, from the start to the end. */
internal fun secretRuns(text: String): List<SecretRun> {
    val runs = mutableListOf<SecretRun>()
    text.forEachIndexed { index, char ->
        val kind = when {
            char.isLetter() -> SecretCharKind.Letter
            char.isDigit() -> SecretCharKind.Digit
            else -> SecretCharKind.Symbol
        }
        val last = runs.lastOrNull()
        if (last != null && last.kind == kind) {
            runs[runs.lastIndex] = last.copy(end = index + 1)
        } else {
            runs += SecretRun(index, index + 1, kind)
        }
    }
    return runs
}

/**
 * A secret shown in clear with its digits in [digit] and its symbols in [symbol]; the letters keep the color
 * of the field. The text stays the same length - the cursor and the selection do not move.
 */
internal data class SecretColorTransformation(val digit: Color, val symbol: Color) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText = TransformedText(
        buildAnnotatedString {
            append(text)
            secretRuns(text.text).forEach { run ->
                when (run.kind) {
                    SecretCharKind.Letter -> Unit
                    SecretCharKind.Digit -> addStyle(SpanStyle(color = digit), run.start, run.end)
                    SecretCharKind.Symbol -> addStyle(SpanStyle(color = symbol), run.start, run.end)
                }
            }
        },
        OffsetMapping.Identity,
    )
}
