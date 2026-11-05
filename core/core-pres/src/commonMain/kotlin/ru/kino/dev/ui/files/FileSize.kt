package ru.kino.dev.ui.files

import androidx.compose.runtime.Composable
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.res.stringResource

/**
 * The size of a file as people read it, in the way of the language (pass-docs, Step 103): "512 B", "340 KB",
 * "1.5 MB" - "1,5 МБ" in Russian. Units of 1024; one digit after the point, none when it is zero.
 */
@Composable
fun fileSizeText(bytes: Long): String {
    val separator = stringResource(Res.string.size_decimal_separator)
    return when {
        bytes < KILOBYTE -> stringResource(Res.string.size_bytes, bytes.toString())
        bytes < MEGABYTE -> stringResource(Res.string.size_kilobytes, oneDigit(bytes, KILOBYTE, separator))
        else -> stringResource(Res.string.size_megabytes, oneDigit(bytes, MEGABYTE, separator))
    }
}

/** [bytes] in [unit]s, rounded to one digit after the [separator]; "2", not "2.0". */
internal fun oneDigit(bytes: Long, unit: Long, separator: String): String {
    val tenths = (bytes * TENTHS + unit / 2) / unit
    val whole = tenths / TENTHS
    val fraction = tenths % TENTHS
    return if (fraction == 0L) whole.toString() else "$whole$separator$fraction"
}

private const val KILOBYTE = 1024L
private const val MEGABYTE = 1024L * 1024
private const val TENTHS = 10L
