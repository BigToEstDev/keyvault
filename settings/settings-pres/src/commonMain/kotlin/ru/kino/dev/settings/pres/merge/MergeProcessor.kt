package ru.kino.dev.settings.pres.merge

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.core.CredentialLimits
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.mvi.textIntent
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.vault.sync.DatabaseCopyEditor
import ru.kino.dev.vault.sync.DatabaseCopyFailure
import ru.kino.dev.vault.sync.MergeFailure
import ru.kino.dev.vault.sync.OtherDatabaseCredentials
import ru.kino.dev.vault.sync.OtherDatabaseEditor
import ru.kino.dev.vault.sync.OtherDatabaseKind
import ru.kino.dev.vault.sync.OtherDatabaseRepository

/**
 * The page `Синхронизация` of the database [dbKey] (pass-docs, Step 108): another file brought in - a copy of this
 * database merged, merged both ways (Step 110 - the result written into the copy's file too), a different database
 * imported, each on its own card. The file of this database itself is refused when picked. The file is picked, its credentials typed -
 * its own, they may differ - and [OtherDatabaseEditor] merges and writes; what came in is said in a dialog.
 *
 * The core checks the file is what the card says before anything changes: the wrong card is said, and the way to
 * the right one. Neither file is kept: picked again next time.
 *
 * Below the three, the file of this database copied as it is by [copier] (Step 111): "Save a copy" into a file picked,
 * "Send" to the share menu of the system where there is one. One thing at a time: a merge or a copy that runs takes
 * no other.
 */
class MergeProcessor(
    private val dbKey: String,
    private val other: OtherDatabaseRepository,
    private val editor: OtherDatabaseEditor,
    private val copier: DatabaseCopyEditor,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<MergeState, MergeState, MergeSideEffect>, MergeActions {

    override val container =
        orbitContainer<MergeState, MergeSideEffect>(MergeState(shareAvailable = copier.isShareAvailable()))

    override fun onBack() {
        intent { navigator.navigate(MviNavEvent.NavigateBack) }
    }

    override fun onDatabasePicked(kind: OtherDatabaseKind, uri: String) {
        intent {
            if (state.busy) return@intent
            // The file of this database itself: said at once, before its credentials are typed (Step 110)
            if (uri == dbKey) return@intent reduce { state.copy(message = MergeMessage.Failed(MergeProblem.OwnFile)) }
            val name = other.name(uri)
            // Another file picked from the open dialog - its name tapped: what was typed stays, only the file changes
            val dialog = state.credentials?.takeIf { it.kind == kind }
                ?.copy(uri = uri, fileName = name, wrongCredentials = false)
                ?: CredentialsDialog(kind, uri, name)
            reduce { state.copy(credentials = dialog, message = null) }
        }
    }

    override fun onPasswordChange(password: String) {
        // Typed text: the state is updated before the field draws again, or the cursor jumps
        textIntent {
            reduce {
                state.editCredentials {
                    if (CredentialLimits.isPasswordTooLong(password)) {
                        it.copy(tooLong = true)
                    } else {
                        it.copy(password = password, tooLong = false, wrongCredentials = false)
                    }
                }
            }
        }
    }

    override fun onTogglePasswordVisible() {
        intent { reduce { state.editCredentials { it.copy(passwordVisible = !it.passwordVisible) } } }
    }

    override fun onKeyFilePicked(uri: String) {
        intent {
            if (state.credentials?.merging != false) return@intent
            val name = other.name(uri)
            reduce { state.editCredentials { it.copy(keyFileUri = uri, keyFileName = name, wrongCredentials = false) } }
        }
    }

    override fun onRemoveKeyFile() {
        intent {
            reduce { state.editCredentials { it.copy(keyFileUri = null, keyFileName = null, wrongCredentials = false) } }
        }
    }

    override fun onConfirm() {
        intent { state.credentials?.takeIf { it.canConfirm }?.let { bringIn(it) } }
    }

    override fun onDismissCredentials() {
        intent { if (state.credentials?.merging == false) reduce { state.copy(credentials = null) } }
    }

    override fun onDismissMessage() {
        intent { reduce { state.copy(message = null) } }
    }

    override fun onSaveCopy() {
        intent {
            if (state.busy) return@intent
            postSideEffect(MergeSideEffect.PickCopyTarget(copier.name(dbKey)))
        }
    }

    override fun onCopyTargetPicked(uri: String) {
        intent {
            copying {
                copier.saveCopy(dbKey, uri)
                reduce { state.copy(copying = false, message = MergeMessage.CopySaved) }
            }
        }
    }

    override fun onShare() {
        intent {
            copying {
                val uri = copier.share(dbKey)
                reduce { state.copy(copying = false) }
                postSideEffect(MergeSideEffect.Share(uri))
            }
        }
    }

    // The loading dialog while the copy runs; a copy not made is said
    private suspend fun Syntax<MergeState, MergeSideEffect>.copying(
        copy: suspend Syntax<MergeState, MergeSideEffect>.() -> Unit,
    ) {
        if (state.busy) return
        reduce { state.copy(copying = true, message = null) }
        try {
            copy()
        } catch (failure: DatabaseCopyFailure) {
            reduce { state.copy(copying = false, message = MergeMessage.CopyFailed(failure.problem)) }
        }
    }

    private suspend fun Syntax<MergeState, MergeSideEffect>.bringIn(dialog: CredentialsDialog) {
        reduce { state.copy(credentials = dialog.copy(merging = true)) }
        val credentials = OtherDatabaseCredentials(
            password = dialog.password.ifEmpty { null },
            keyFileUri = dialog.keyFileUri,
        )
        try {
            val summary = editor.bringIn(dbKey, dialog.kind, dialog.uri, credentials)
            reduce { state.copy(credentials = null, message = MergeMessage.Done(summary)) }
        } catch (failure: MergeFailure) {
            failed(dialog, failure)
        }
    }

    private suspend fun Syntax<MergeState, MergeSideEffect>.failed(dialog: CredentialsDialog, failure: MergeFailure) {
        val problem = when (failure) {
            // Typed again in the same dialog: said under the field
            is MergeFailure.WrongCredentials ->
                return reduce { state.copy(credentials = dialog.copy(merging = false, wrongCredentials = true)) }
            // The lock took the database meanwhile: the settings are gone with `vault`, nobody to tell
            is MergeFailure.Locked -> return
            is MergeFailure.NotSameDatabase -> MergeProblem.NotSameDatabase
            is MergeFailure.SameDatabase -> MergeProblem.SameDatabase
            is MergeFailure.CorruptedFile, is MergeFailure.UnsupportedFile -> MergeProblem.Damaged
            is MergeFailure.FileUnreachable -> MergeProblem.Unreachable
            is MergeFailure.KeyFileTooLarge -> MergeProblem.KeyFileTooLarge
            is MergeFailure.KeyFileUnreadable -> MergeProblem.KeyFileUnreachable
            is MergeFailure.NotSaved -> MergeProblem.NotSaved
            is MergeFailure.OwnFile -> MergeProblem.OwnFile
            is MergeFailure.CopyNotWritten -> MergeProblem.CopyNotWritten
            is MergeFailure.CopyNotWrittenChangedElsewhere -> MergeProblem.CopyNotWrittenChangedElsewhere
            is MergeFailure.CredentialsChanged, is MergeFailure.Unexpected -> {
                log.unexpected(TAG, failure)
                MergeProblem.Unexpected
            }
        }
        // Merged, not written all the way - done all the same, the dialog goes; else it stays to try another file or key
        val keep = problem !in MERGED
        reduce {
            state.copy(
                credentials = if (keep) dialog.copy(merging = false) else null,
                message = MergeMessage.Failed(problem),
            )
        }
    }

    private fun MergeState.editCredentials(change: (CredentialsDialog) -> CredentialsDialog): MergeState =
        credentials?.takeIf { !it.merging }?.let { copy(credentials = change(it)) } ?: this

    private companion object {
        const val TAG = "Merge"

        /** The merge is made, only a write failed: nothing to try again in the dialog. */
        val MERGED = setOf(
            MergeProblem.NotSaved,
            MergeProblem.CopyNotWritten,
            MergeProblem.CopyNotWrittenChangedElsewhere,
        )
    }
}
