package ru.kino.dev.vault.lock

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * One change of the database at a time, and locking never in the middle of one.
 *
 * A change is two calls - made in the core, then written to the file - and a lock between them would leave
 * the change in memory with the file behind it. The editors make their changes inside [exclusiveChange], and
 * [AutoLock] locks inside it: a lock waits for the write to end (pass-docs, Step 46).
 *
 * A change once begun is not cancelled: the screen that asked for it may be gone (back, another page of a
 * container), but the change is already in memory and the file has to follow it (pass-docs, Step 114).
 * Waiting for the guard stays cancellable - nothing has been changed yet then.
 *
 * One for the app: the editors and the lock have to share it.
 */
class DatabaseGuard {
    private val mutex = Mutex()

    /**
     * The guard for work that reads before it changes - a file of another provider, which may take long and
     * should go with the screen. Wrap the change itself in [unstoppable].
     */
    suspend fun <T> exclusive(block: suspend () -> T): T = mutex.withLock { block() }

    /** The guard for a change from its first step: it runs to the end, whoever cancels. */
    suspend fun <T> exclusiveChange(block: suspend () -> T): T = exclusive { unstoppable(block) }
}

/**
 * Runs [block] to the end even if the caller is cancelled - the cancellation is seen once it returns. For the
 * change and the write of a database inside [DatabaseGuard.exclusive].
 */
suspend fun <T> unstoppable(block: suspend () -> T): T = withContext(NonCancellable) { block() }
