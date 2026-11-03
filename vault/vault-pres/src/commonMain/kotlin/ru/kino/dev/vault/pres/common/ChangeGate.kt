package ru.kino.dev.vault.pres.common

import kotlin.concurrent.Volatile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import ru.kino.dev.vault.changes.VaultChanges

/**
 * The changes of the database [dbKey] a processor has to read again on: made elsewhere - by the screen beside it, in
 * a container - and not read yet (pass-docs, Step 98).
 *
 * Its own writes the processor wraps in [own]: their change is its own to follow up on - read again, or leave the
 * screen - and reading again on it as well, at the same time, would go back twice. [reading] at the start of every
 * read: what is read is the version of now, or newer.
 */
class ChangeGate(private val changes: VaultChanges, private val dbKey: String) {

    /** One own write at a time: the processors guard their writes with a flag of the state. */
    @Volatile
    private var owning = false

    /** The version read last, or covered by an own write. What the screen first reads is there already. */
    @Volatile
    private var seen = changes.current(dbKey)

    /** Each change from elsewhere not read yet. */
    val elsewhere: Flow<Unit> = changes.versions(dbKey).filter { !owning && it > seen }.map { }

    /** Before reading the database. */
    fun reading() {
        seen = changes.current(dbKey)
    }

    /** An own write - and what follows from it: [block] reads again or leaves the screen itself. */
    suspend fun <T> own(block: suspend () -> T): T {
        owning = true
        try {
            return block()
        } finally {
            seen = maxOf(seen, changes.current(dbKey))
            owning = false
        }
    }
}
