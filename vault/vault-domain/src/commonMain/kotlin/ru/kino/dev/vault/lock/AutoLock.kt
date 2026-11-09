package ru.kino.dev.vault.lock

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ru.kino.dev.core.CoreException
import ru.kino.dev.settings.lock.InactivityTimer
import ru.kino.dev.vault.VaultDatabaseRepository

/**
 * Locks the open database when the [timer] of the settings says nobody touched the app long enough
 * (pass-docs, Step 46, Step 59). The count is the timer's; here - what a lock is.
 *
 * Locking waits for a change being written ([DatabaseGuard]). Once locked, the database is forgotten here
 * until `vault` shows it again ([watch]); [locked] tells the app to put the locked screen up, and
 * [covering] says the screens of `vault` are not to be seen meanwhile.
 *
 * @param onFailure a lock the core refused; the database stays open and is not watched any more
 */
class AutoLock(
    private val database: VaultDatabaseRepository,
    private val guard: DatabaseGuard,
    private val timer: InactivityTimer,
    private val scope: CoroutineScope,
    private val onFailure: (CoreException) -> Unit,
) {
    private val _covering = MutableStateFlow(false)

    /**
     * A lock is under way or done, and `vault` is not to be seen: from the lock until `vault` shows the
     * database again.
     */
    val covering: StateFlow<Boolean> = _covering.asStateFlow()

    private val _locked = Channel<String>(Channel.BUFFERED)

    /** The dbKey of a database just locked - once each, kept until somebody takes it. */
    val locked: Flow<String> = _locked.receiveAsFlow()

    private val state = Any()

    @Volatile
    private var dbKey: String? = null

    init {
        timer.setOnExpired(::expired)
    }

    /** `vault` shows the database [dbKey]: from now on it is locked once the timer runs out. */
    fun watch(dbKey: String) {
        synchronized(state) {
            this.dbKey = dbKey
            _covering.value = false
        }
        timer.start()
    }

    /** Locks now, not waiting for the timer. False when there was nothing to lock, or the core refused. */
    suspend fun lockNow(): Boolean {
        val key = dbKey ?: return false
        _covering.value = true
        // In the scope of the lock, not of the caller: the screen asking goes away with the lock it asked for
        return scope.async { lock(key) }.await()
    }

    // Told on the thread that saw it - coming back included, before the screens are drawn: covered at once
    private fun expired() {
        val key = dbKey ?: return
        _covering.value = true
        scope.launch { lock(key) }
    }

    private suspend fun lock(key: String): Boolean {
        synchronized(state) {
            // Locked already, by another way to it, or another database is open now
            if (dbKey != key) return false
            dbKey = null
        }
        timer.stop()
        return try {
            guard.exclusiveChange { database.lock(key) }
            _locked.send(key)
            true
        } catch (e: CoreException) {
            _covering.value = false
            onFailure(e)
            false
        }
    }
}
