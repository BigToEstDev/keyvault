package ru.kino.dev.vault.pres.sync

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.core.MergeSummary
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.vault.sync.AppReturns
import ru.kino.dev.vault.sync.MergeFailure
import ru.kino.dev.vault.sync.SaveStatus
import ru.kino.dev.vault.sync.SyncRequests
import ru.kino.dev.vault.sync.VaultSync
import ru.kino.dev.vault.sync.nothingChanged

/**
 * The sync of the open database over all the screens of `vault` - one for as long as `vault` is shown, from opening
 * or unlocking to locking or closing (pass-docs, Step 108).
 *
 * - A save that found the file written elsewhere wrote nothing ([SaveStatus.postponed]): "Synchronize" or "Cancel" -
 *   every such save asks anew. "Synchronize" merges and saves ([VaultSync.synchronize]); "Cancel" leaves the change in
 *   memory, and the main screen says so with a bar of its own, which asks through [SyncRequests].
 * - Quietly, whenever it is shown again - `vault` comes up, the app comes back on android - and when the window is
 *   shown again on the desktop ([AppReturns]): a file written elsewhere is merged and saved ([VaultSync.check]) -
 *   unless something of ours waits in memory.
 *
 * What came in, and what failed, is a dialog closed by "OK", never a snackbar; nothing came in - nothing is shown. A
 * failure of the quiet check is said once for each kind: coming back again would say it again and again.
 */
class SyncProcessor(
    private val dbKey: String,
    private val sync: VaultSync,
    private val status: SaveStatus,
    private val requests: SyncRequests,
    private val returns: AppReturns,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<SyncState, SyncState, Nothing>, SyncActions {

    /** What the quiet check has said already. */
    private val saidQuietly = mutableSetOf<SyncProblem>()

    override val container = orbitContainer<SyncState, Nothing>(SyncState()) {
        repeatOnSubscription {
            // Shown again - `vault` has just come up, opened or unlocked, or the app is back on android: the screens
            // listen again only after the app was told it came back, so that signal may find nobody listening
            check()
            merge(
                status.postponed.filter { it == dbKey }.map { Signal.Ask },
                requests.requests.filter { it == dbKey }.map { Signal.Synchronize },
                returns.returns.map { Signal.Check },
            ).collect { signal ->
                when (signal) {
                    Signal.Ask -> ask()
                    Signal.Synchronize -> synchronize()
                    Signal.Check -> check()
                }
            }
        }
    }

    override fun onSynchronize() {
        intent { synchronize() }
    }

    override fun onCancel() {
        intent { if (state.dialog == SyncDialog.Ask) reduce { state.copy(dialog = null) } }
    }

    override fun onDismiss() {
        intent { reduce { state.copy(dialog = null) } }
    }

    // A dialog already up stays: what it says is not taken away by the next question
    private suspend fun Syntax<SyncState, Nothing>.ask() {
        if (state.dialog == null && !state.syncing) reduce { state.copy(dialog = SyncDialog.Ask) }
    }

    private suspend fun Syntax<SyncState, Nothing>.synchronize() {
        if (state.syncing) return
        reduce { state.copy(dialog = null, syncing = true) }
        val dialog = try {
            done(sync.synchronize(dbKey))
        } catch (failure: MergeFailure) {
            failed(failure)?.let { SyncDialog.Failed(it) }
        }
        reduce { state.copy(dialog = dialog, syncing = false) }
    }

    private suspend fun Syntax<SyncState, Nothing>.check() {
        if (state.syncing) return
        val dialog = try {
            sync.check(dbKey, onMerging = { reduce { state.copy(syncing = true) } })?.let { done(it) }
        } catch (failure: MergeFailure) {
            failed(failure)?.takeIf { saidQuietly.add(it) }?.let { SyncDialog.Failed(it) }
        }
        reduce { state.copy(dialog = state.dialog ?: dialog, syncing = false) }
    }

    private fun done(summary: MergeSummary): SyncDialog? = if (summary.nothingChanged) null else SyncDialog.Done(summary)

    // Locked meanwhile - `vault` is gone with the lock, there is nobody to tell
    private fun failed(failure: MergeFailure): SyncProblem? = when (failure) {
        is MergeFailure.Locked -> null
        is MergeFailure.CredentialsChanged -> SyncProblem.CredentialsChanged
        is MergeFailure.NotSameDatabase -> SyncProblem.NotSameDatabase
        is MergeFailure.CorruptedFile, is MergeFailure.UnsupportedFile -> SyncProblem.Damaged
        is MergeFailure.FileUnreachable -> SyncProblem.Unreachable
        is MergeFailure.NotSaved -> SyncProblem.NotSaved
        // The file of the open database is read with its own key: the rest is not expected here
        else -> {
            log.unexpected(TAG, failure)
            SyncProblem.Unexpected
        }
    }

    private enum class Signal { Ask, Synchronize, Check }

    private companion object {
        const val TAG = "Sync"
    }
}
