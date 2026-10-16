package ru.kino.dev.vault.entry

import ru.kino.dev.vault.VaultDatabaseRepository
import kotlin.coroutines.cancellation.CancellationException

/**
 * Writes the file after a change of an entry or of its history. The change is made in memory already: a
 * write that fails is [EntryFailure.NotSaved] - the change waits for the next save. Cancellation passes.
 */
internal suspend fun VaultDatabaseRepository.saveChange(dbKey: String) {
    try {
        save(dbKey)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw EntryFailure.NotSaved(e)
    }
}
