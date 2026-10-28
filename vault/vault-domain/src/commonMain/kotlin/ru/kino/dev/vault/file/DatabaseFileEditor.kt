package ru.kino.dev.vault.file

import ru.kino.dev.core.Compression
import ru.kino.dev.vault.VaultDatabaseRepository
import ru.kino.dev.vault.lock.DatabaseGuard
import kotlin.coroutines.cancellation.CancellationException

/**
 * Changing the file of the open database from its settings: one choice, one write of the file (pass-docs,
 * Step 63). The same value again is not a change: nothing is written.
 *
 * Each change runs inside [guard], so a lock waits for its write to end.
 *
 * Throws [DatabaseFileFailure] and nothing else.
 */
class DatabaseFileEditor(
    private val files: DatabaseFileRepository,
    private val database: VaultDatabaseRepository,
    private val guard: DatabaseGuard = DatabaseGuard(),
) {

    /**
     * Renames the database - its name inside the file, not the file - and saves; returns the name kept, without
     * the spaces around it.
     */
    suspend fun rename(dbKey: String, name: String): String = guard.exclusive {
        val normalized = name.trim()
        if (normalized.isEmpty()) throw DatabaseFileFailure.EmptyName()
        val current = files.read(dbKey)
        if (normalized != current.name) {
            files.change(dbKey, normalized, current.compression)
            save(dbKey)
        }
        normalized
    }

    /** Packs the database with [compression] and saves - the file is written with it at once. */
    suspend fun setCompression(dbKey: String, compression: Compression): Unit = guard.exclusive {
        val current = files.read(dbKey)
        if (compression == current.compression) return@exclusive
        files.change(dbKey, current.name, compression)
        save(dbKey)
    }

    private suspend fun save(dbKey: String) {
        try {
            database.save(dbKey)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw DatabaseFileFailure.NotSaved(e)
        }
    }
}
