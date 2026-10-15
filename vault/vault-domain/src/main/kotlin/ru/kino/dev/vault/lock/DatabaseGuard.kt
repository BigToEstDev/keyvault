package ru.kino.dev.vault.lock

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * One change of the database at a time, and locking never in the middle of one.
 *
 * A change is two calls - made in the core, then written to the file - and a lock between them would leave
 * the change in memory with the file behind it. The editors make their changes inside [exclusive], and
 * [AutoLock] locks inside it: a lock waits for the write to end (pass-docs, Step 46).
 *
 * One for the app: the editors and the lock have to share it.
 */
class DatabaseGuard {
    private val mutex = Mutex()

    suspend fun <T> exclusive(block: suspend () -> T): T = mutex.withLock { block() }
}
