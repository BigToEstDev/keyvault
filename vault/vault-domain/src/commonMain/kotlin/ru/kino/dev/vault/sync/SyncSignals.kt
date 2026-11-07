package ru.kino.dev.vault.sync

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import ru.kino.dev.core.MergeSummary

/**
 * "Synchronize" asked for outside the sync's own dialog - from the bar on the main screen after "Cancel" (pass-docs,
 * Step 108). One for the app: the screen asks, the sync of `vault` answers.
 */
class SyncRequests {

    private val asked = signal<String>()

    /** The dbKey of each database asked to be synchronized now. */
    val requests: SharedFlow<String> = asked.asSharedFlow()

    fun synchronize(dbKey: String) {
        asked.tryEmit(dbKey)
    }
}

/**
 * The app is on the screen again - brought back on android, the window shown again on the desktop; what the
 * inactivity timer of the settings is told as well (pass-docs, Step 108). One for the app: the app tells, the quiet
 * check of the sync listens.
 */
class AppReturns {

    private val returned = signal<Unit>()

    val returns: SharedFlow<Unit> = returned.asSharedFlow()

    fun cameBack() {
        returned.tryEmit(Unit)
    }
}

/**
 * What came in, as a person sees it - groups and entries together; moved is changed (pass-docs, Step 108).
 */
data class MergeCounts(val added: Int, val changed: Int, val deleted: Int) {
    val none: Boolean = added + changed + deleted == 0
}

val MergeSummary.counts: MergeCounts
    get() = MergeCounts(
        added = addedGroups + addedEntries,
        changed = updatedGroups + updatedEntries + movedGroups + movedEntries,
        deleted = deletedGroups + deletedEntries,
    )

/** Nothing came in from the file: no group, no entry, no setting of the database. */
val MergeSummary.nothingChanged: Boolean
    get() = counts.none && !metaDataChanged

// Never suspends the one who tells: nobody listening, the signal is gone - there was nobody to answer it
private fun <T> signal() = MutableSharedFlow<T>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
