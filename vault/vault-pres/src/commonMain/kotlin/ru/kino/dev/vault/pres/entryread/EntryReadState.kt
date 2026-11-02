package ru.kino.dev.vault.pres.entryread

import ru.kino.dev.core.FieldProtection
import ru.kino.dev.vault.entry.EntryContent
import ru.kino.dev.vault.entry.EntryTimes
import ru.kino.dev.vault.pres.common.GroupChoice

/**
 * @param loading the entry is not read yet - the skeleton is shown
 * @param content the entry; null until it is read
 * @param group the group the entry is in, with the path down to it; null until it is read
 * @param times when the entry was made and last changed; empty until it is read
 * @param shownOwn indexes of protected own fields shown in clear
 * @param protection the fields private by the database (pass-docs, Step 65); only the password until read
 * @param shownKeys keys of the fields of the preset private by the database shown in clear
 * @param delete the dialog asking to delete the entry
 * @param locking the lock asked by the lock button is under way
 * @param message for the snackbar, until it takes it
 */
data class EntryReadState(
    val loading: Boolean = true,
    val content: EntryContent? = null,
    val group: GroupChoice? = null,
    val times: EntryTimes = EntryTimes(),
    val passwordVisible: Boolean = false,
    val shownOwn: Set<Int> = emptySet(),
    val protection: FieldProtection = FieldProtection.DEFAULT,
    val shownKeys: Set<String> = emptySet(),
    val delete: DeleteDialog? = null,
    val locking: Boolean = false,
    val message: EntryReadMessage? = null,
)

/**
 * "Delete the entry?" - the entry called [title] as the file holds it.
 *
 * @param toBin the delete goes to the recycle bin; false - for good (pass-docs, Step 70)
 * @param deleting the delete is under way: the dialog shows progress and takes nothing
 */
data class DeleteDialog(val title: String, val toBin: Boolean, val deleting: Boolean = false)

/** What the snackbar says. */
enum class EntryReadMessage { LoadFailed, NotSaved, Unexpected }
