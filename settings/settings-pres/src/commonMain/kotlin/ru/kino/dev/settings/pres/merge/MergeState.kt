package ru.kino.dev.settings.pres.merge

import ru.kino.dev.core.MergeSummary
import ru.kino.dev.vault.sync.DatabaseCopyFailure
import ru.kino.dev.vault.sync.OtherDatabaseKind

/**
 * The page `Синхронизация` (pass-docs, Step 108, 110): three cards, then for the file picked - the dialog of its credentials,
 * then what came in or what failed. Below them the file of this database copied as it is (Step 111) - "Save a copy",
 * and "Send" where the platform hands files to other apps.
 *
 * @property shareAvailable "Send" is shown - android, not the desktop
 * @property copying the file of this database is being copied: the loading dialog, nothing takes input
 */
data class MergeState(
    val credentials: CredentialsDialog? = null,
    val message: MergeMessage? = null,
    val shareAvailable: Boolean = false,
    val copying: Boolean = false,
) {
    /** A merge or a copy runs: another is not started. */
    val busy: Boolean = copying || credentials?.merging == true
}

/**
 * The credentials of the other database, for the file [uri] picked on the card of [kind].
 *
 * @property fileName as the provider tells it; null - it does not
 * @property wrongCredentials they did not open the database: said under the field, the dialog stays
 * @property tooLong the password typed past the limit of the core - not taken
 * @property merging the merge and its write run: the button shows progress, nothing takes input
 */
data class CredentialsDialog(
    val kind: OtherDatabaseKind,
    val uri: String,
    val fileName: String?,
    val password: String = "",
    val passwordVisible: Boolean = false,
    val keyFileUri: String? = null,
    val keyFileName: String? = null,
    val wrongCredentials: Boolean = false,
    val tooLong: Boolean = false,
    val merging: Boolean = false,
) {
    /** A database opens with a password, a key file, or both - one is enough to try. */
    val canConfirm: Boolean = !merging && (password.isNotEmpty() || keyFileUri != null)

    // The password stays out of logs and crash reports
    override fun toString(): String =
        "CredentialsDialog(kind=$kind, password=${if (password.isEmpty()) "none" else "***"}, keyFile=${keyFileUri != null}, " +
            "wrong=$wrongCredentials, merging=$merging)"
}

/** Said in a dialog closed by "OK", never a snackbar. */
sealed interface MergeMessage {

    /** What came in. */
    data class Done(val summary: MergeSummary) : MergeMessage

    data class Failed(val problem: MergeProblem) : MergeMessage

    /** "Save a copy" wrote the file picked (Step 111). */
    data object CopySaved : MergeMessage

    /** "Save a copy" or "Send" made no copy (Step 111). */
    data class CopyFailed(val problem: CopyProblem) : MergeMessage
}

/** Every failure of a copy the page says (Step 111) - one for each [DatabaseCopyFailure]. */
enum class CopyProblem {
    /** The file picked to save into is the file of this database. */
    OwnFile,

    /** A change waited for its save, and the save failed. */
    NotSaved,

    /** The file of this database was written elsewhere - synced first. */
    ChangedElsewhere,

    /** The file of this database cannot be read. */
    Unreadable,

    /** The copy cannot be written. */
    NotWritten,
}

/** What the page says for the failure. */
val DatabaseCopyFailure.problem: CopyProblem
    get() = when (this) {
        is DatabaseCopyFailure.OwnFile -> CopyProblem.OwnFile
        is DatabaseCopyFailure.NotSaved -> CopyProblem.NotSaved
        is DatabaseCopyFailure.ChangedElsewhere -> CopyProblem.ChangedElsewhere
        is DatabaseCopyFailure.Unreadable -> CopyProblem.Unreadable
        is DatabaseCopyFailure.NotWritten -> CopyProblem.NotWritten
    }

/** Every failure the page says - wrong credentials are said in the dialog itself. */
enum class MergeProblem {
    /** "Merge with a copy", but the file holds another database - that is "Import". */
    NotSameDatabase,

    /** "Import", but the file holds a copy of this database - that is "Merge". */
    SameDatabase,

    /** Damaged, or not a database the app reads. */
    Damaged,

    /** The file cannot be read. */
    Unreachable,

    KeyFileTooLarge,

    KeyFileUnreachable,

    /** Merged, but the file of this database is not written: the merge waits in memory. */
    NotSaved,

    /** The file picked is the file of this database (Step 110). */
    OwnFile,

    /** Both ways: merged and written here, the copy's file not written. */
    CopyNotWritten,

    /** Both ways: the file of this database was changed elsewhere - not written, the copy left as it was. */
    CopyNotWrittenChangedElsewhere,

    Unexpected,
}
