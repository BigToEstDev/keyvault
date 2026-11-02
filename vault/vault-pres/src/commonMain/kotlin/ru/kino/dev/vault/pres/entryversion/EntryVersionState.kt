package ru.kino.dev.vault.pres.entryversion

import ru.kino.dev.core.FieldProtection
import ru.kino.dev.vault.entry.OpenedEntry

/**
 * @param loading the version is not read yet - the skeleton is shown
 * @param version the version laid over its preset, with when it was saved; null until it is read
 * @param shownOwn indexes of protected own fields shown in clear
 * @param protection the fields private by the database (pass-docs, Step 65); only the password until read
 * @param shownKeys keys of the fields of the preset private by the database shown in clear
 * @param restoring null - no dialog; false - "restore the version?" asked; true - the restore is under way
 * @param message for the snackbar, until it takes it
 */
data class EntryVersionState(
    val loading: Boolean = true,
    val version: OpenedEntry? = null,
    val passwordVisible: Boolean = false,
    val shownOwn: Set<Int> = emptySet(),
    val protection: FieldProtection = FieldProtection.DEFAULT,
    val shownKeys: Set<String> = emptySet(),
    val restoring: Boolean? = null,
    val message: EntryVersionMessage? = null,
)

/** What the snackbar says. */
enum class EntryVersionMessage { LoadFailed, NotSaved, Unexpected }
