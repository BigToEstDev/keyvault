package ru.kino.dev.ui.components

import kotlin.math.log2

/** How hard a password is to guess, by its length and its kinds of characters (pass-docs, Step 74). */
enum class PasswordStrength { Weak, Medium, Strong }

/**
 * The strength of [value]; null for an empty one - nothing to tell. Bits = length × log₂ of the alphabet, the
 * alphabet being the sum of the kinds of characters the password has. A rough count with no dictionary: `Password123`
 * reads stronger than it is - accepted for a hint that only draws (owner, 24.10.26).
 */
fun passwordStrength(value: String): PasswordStrength? {
    if (value.isEmpty()) return null
    val bits = value.length * log2(alphabetOf(value).toDouble())
    return when {
        bits < MEDIUM_FROM -> PasswordStrength.Weak
        bits < STRONG_FROM -> PasswordStrength.Medium
        else -> PasswordStrength.Strong
    }
}

/** The size of the alphabet [value] is taken from: each kind of character it has adds the whole kind. */
internal fun alphabetOf(value: String): Int =
    CharKind.entries.filter { kind -> value.any(kind.has) }.sumOf { it.size }

/** The kinds of characters and how many each holds. */
private enum class CharKind(val size: Int, val has: (Char) -> Boolean) {
    LatinLower(26, { it in 'a'..'z' }),
    LatinUpper(26, { it in 'A'..'Z' }),
    Digit(10, { it in '0'..'9' }),

    /** The rest of printable ASCII: the space and the punctuation. */
    Symbol(33, { it.code in ' '.code..'~'.code && !it.isLetterOrDigit() }),
    CyrillicLower(33, { it in 'а'..'я' || it == 'ё' }),
    CyrillicUpper(33, { it in 'А'..'Я' || it == 'Ё' }),

    /** Anything else - a letter of another script, an emoji: a large alphabet, counted once. */
    Other(100, { it.code > '~'.code && it !in 'а'..'я' && it !in 'А'..'Я' && it != 'ё' && it != 'Ё' }),
}

/** From these many bits a password is medium, then strong (owner, 24.10.26). */
private const val MEDIUM_FROM = 40.0
private const val STRONG_FROM = 80.0
