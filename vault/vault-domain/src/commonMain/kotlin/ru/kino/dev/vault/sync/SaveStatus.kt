package ru.kino.dev.vault.sync

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** How the open database stands to its file - whether everything in memory is written (pass-docs, Step 108). */
enum class SaveState {
    /** All in memory is in the file. */
    Saved,

    /**
     * A save found the file written elsewhere and did not write: the change waits in memory for a sync. The ui
     * asks, and after "Cancel" says so on the main screen.
     */
    ChangedElsewhere,

    /** A write failed: the change waits in memory for the next save. */
    NotSaved,
}

/**
 * [SaveState] of each open database, for the app: the save sets it, the sync and the screens read it (pass-docs,
 * Step 108). In memory only - a database opened again starts [SaveState.Saved].
 */
class SaveStatus {

    private val states = MutableStateFlow<Map<String, SaveState>>(emptyMap())

    private val postponedSaves = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /**
     * The dbKey of each save that found the file written elsewhere and did not write - every one of them, so each
     * is asked about anew, not only the first.
     */
    val postponed: SharedFlow<String> = postponedSaves.asSharedFlow()

    fun state(dbKey: String): Flow<SaveState> = states.map { it[dbKey] ?: SaveState.Saved }.distinctUntilChanged()

    fun current(dbKey: String): SaveState = states.value[dbKey] ?: SaveState.Saved

    /** Everything in memory is written. */
    fun saved(dbKey: String) = set(dbKey, SaveState.Saved)

    /** The save did not write: the file is somebody else's now. */
    fun changedElsewhere(dbKey: String) {
        set(dbKey, SaveState.ChangedElsewhere)
        postponedSaves.tryEmit(dbKey)
    }

    /** The write failed. */
    fun notSaved(dbKey: String) = set(dbKey, SaveState.NotSaved)

    /** The database is closed: nothing of it waits any more. */
    fun forget(dbKey: String) = states.update { it - dbKey }

    private fun set(dbKey: String, state: SaveState) = states.update { it + (dbKey to state) }
}
