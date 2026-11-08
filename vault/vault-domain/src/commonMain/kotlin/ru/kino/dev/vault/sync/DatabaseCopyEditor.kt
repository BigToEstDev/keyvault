package ru.kino.dev.vault.sync

import kotlinx.coroutines.CancellationException
import ru.kino.dev.vault.VaultDatabaseRepository
import ru.kino.dev.vault.lock.DatabaseGuard

/**
 * The file of the open database copied as it is (pass-docs, Step 111): "Save a copy" - into a file the person picks -
 * and "Send" - handed to another app. The file is copied, not the database written anew: the same password, key file
 * and cipher, the same bytes.
 *
 * The file must hold what is in memory: a change that waits for its save is saved first; a file written elsewhere
 * holds another version - refused, synced first. Runs inside [guard], so no save writes the file while it is read.
 *
 * Throws [DatabaseCopyFailure] and nothing else.
 */
class DatabaseCopyEditor(
    private val copies: DatabaseCopyRepository,
    private val database: VaultDatabaseRepository,
    private val status: SaveStatus,
    private val guard: DatabaseGuard,
) {

    /** The name to suggest for the copy - the file's own; [FALLBACK_NAME] when the provider does not tell it. */
    suspend fun name(dbKey: String): String = copies.name(dbKey) ?: FALLBACK_NAME

    /** Whether "Send" is there at all - see [DatabaseCopyRepository.isShareAvailable]. */
    fun isShareAvailable(): Boolean = copies.isShareAvailable()

    /** Writes the file of [dbKey] into the file at [uri]. */
    suspend fun saveCopy(dbKey: String, uri: String) {
        // The same uri only: one file may come under different uris on android, not caught (as Step 110)
        if (uri == dbKey) throw DatabaseCopyFailure.OwnFile()
        val bytes = guard.exclusive { written(dbKey) }
        copies.write(uri, bytes)
    }

    /** Puts the file of [dbKey] where another app can read it, and returns the uri to hand it. */
    suspend fun share(dbKey: String): String {
        val bytes = guard.exclusive { written(dbKey) }
        return copies.prepareShare(name(dbKey), bytes)
    }

    // The bytes of the file once it holds what is in memory
    private suspend fun written(dbKey: String): ByteArray {
        when (status.current(dbKey)) {
            SaveState.Saved -> Unit
            SaveState.ChangedElsewhere -> throw DatabaseCopyFailure.ChangedElsewhere()
            SaveState.NotSaved -> save(dbKey)
        }
        return copies.read(dbKey)
    }

    private suspend fun save(dbKey: String) {
        try {
            database.save(dbKey)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw DatabaseCopyFailure.NotSaved(e)
        }
        // The save found the file written elsewhere and did not write it: the sync of `vault` asks
        if (status.current(dbKey) == SaveState.ChangedElsewhere) throw DatabaseCopyFailure.ChangedElsewhere()
    }

    companion object {
        /** The name of a copy whose file the provider gives no name. */
        const val FALLBACK_NAME = "database.kdbx"
    }
}
