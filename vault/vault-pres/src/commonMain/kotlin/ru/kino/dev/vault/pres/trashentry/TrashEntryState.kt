package ru.kino.dev.vault.pres.trashentry

import ru.kino.dev.core.FieldProtection
import ru.kino.dev.vault.entry.OpenedEntry
import ru.kino.dev.vault.pres.trash.TrashDialog
import ru.kino.dev.vault.trash.TrashEntry

/**
 * @param entry the entry laid over its preset, with when it was made and changed; null until read - a skeleton
 * @param trashed when it went to the bin and where from; null until read
 * @param shownOwn indexes of protected own fields shown in clear
 * @param protection the fields private by the database (pass-docs, Step 65); only the password until read
 * @param shownKeys keys of the fields of the preset private by the database shown in clear
 * @param dialog "restore" or "delete for good?", if asked
 * @param notSaved the change is made, but the file was not written - said in a dialog, then back to the bin
 * @param message for the snackbar, until it takes it
 */
data class TrashEntryState(
    val entry: OpenedEntry? = null,
    val trashed: TrashEntry? = null,
    val passwordVisible: Boolean = false,
    val shownOwn: Set<Int> = emptySet(),
    val protection: FieldProtection = FieldProtection.DEFAULT,
    val shownKeys: Set<String> = emptySet(),
    val dialog: TrashDialog? = null,
    val notSaved: Boolean = false,
    val message: TrashEntryMessage? = null,
) {
    val loading: Boolean = entry == null
}

/** What the snackbar says. */
enum class TrashEntryMessage { LoadFailed, Unexpected }
