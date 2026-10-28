package ru.kino.dev.vault.pres.common

import ru.kino.dev.core.FieldProtection
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.vault.entries.EntrySettingsFailure
import ru.kino.dev.vault.entries.EntrySettingsRepository
import ru.kino.dev.vault.entry.EntryField

/**
 * The private fields of the database (pass-docs, Step 65). Not read - only the password, as a new database has it,
 * and the cause to the log: the entry is shown all the same.
 */
internal suspend fun EntrySettingsRepository.protectionOrDefault(
    dbKey: String,
    log: ErrorLog,
    tag: String,
): FieldProtection = try {
    read(dbKey).protection
} catch (failure: EntrySettingsFailure) {
    log.unexpected(tag, failure)
    FieldProtection.DEFAULT
}

/** An own field is hidden when it is protected in the file or private by the database. */
internal fun FieldProtection.hides(field: EntryField): Boolean = field.protected || protects(field.key)

/** [item] added, or taken away when it is there - what is shown in clear, after a tap on the eye. */
internal fun <T> Set<T>.toggled(item: T): Set<T> = if (item in this) this - item else this + item
