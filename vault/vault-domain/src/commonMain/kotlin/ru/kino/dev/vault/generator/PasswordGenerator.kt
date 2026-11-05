package ru.kino.dev.vault.generator

import ru.kino.dev.core.PasswordOptions

/**
 * The password the dice of the form puts into the field "Password" (pass-docs, Step 104): strong, no choice yet -
 * 16 characters of digits, lower and upper case letters and symbols, each of them there, without those that look
 * alike. A choice of these - later, in the settings.
 */
val GENERATED_PASSWORD: PasswordOptions = PasswordOptions(
    length = 16,
    numbers = true,
    lowercaseLetters = true,
    uppercaseLetters = true,
    symbols = true,
    spaces = false,
    excludeSimilarCharacters = true,
    strict = true,
)

/**
 * A new password for an entry, by the core's generator with [GENERATED_PASSWORD].
 *
 * Throws [ru.kino.dev.vault.entry.EntryFailure.Unexpected] and nothing else: the generator refusing is a bug.
 */
interface PasswordGenerator {

    suspend fun generate(): String
}
