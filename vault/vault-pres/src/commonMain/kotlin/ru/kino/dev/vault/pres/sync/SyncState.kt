package ru.kino.dev.vault.pres.sync

import ru.kino.dev.core.MergeSummary

/**
 * The sync of `vault` over its screens (pass-docs, Step 108).
 *
 * @property syncing a merge is under way - seconds of key derivation; the screens work meanwhile
 */
data class SyncState(
    val dialog: SyncDialog? = null,
    val syncing: Boolean = false,
)

/** The one dialog of the sync shown, if any. */
sealed interface SyncDialog {

    /** A save found the file written elsewhere and wrote nothing: "Synchronize" or "Cancel". */
    data object Ask : SyncDialog

    /** What came in from the file - closed by "OK". Not shown when nothing did. */
    data class Done(val summary: MergeSummary) : SyncDialog

    /** What failed - closed by "OK". */
    data class Failed(val problem: SyncProblem) : SyncDialog
}

/** Every failure of a sync as the dialog says it. */
enum class SyncProblem {
    /** The file was saved elsewhere with other credentials: merged through the settings. */
    CredentialsChanged,

    /** Another database is in the file now. */
    NotSameDatabase,

    /** The file is damaged, or not a database the app reads. */
    Damaged,

    /** The file cannot be read now. */
    Unreachable,

    /** Merged, but the file is not written: the merge waits in memory. */
    NotSaved,

    /** Anything else - to the log. */
    Unexpected,
}
