package ru.kino.dev.vault.pres.trashentry

import ru.kino.dev.vault.group.GroupId

sealed interface TrashEntryAction {

    /** The screen is shown: the deleted entry is read. */
    data object Load : TrashEntryAction

    data object Back : TrashEntryAction

    data object TogglePassword : TrashEntryAction

    /** Shows or hides the value of the protected own field at [index]. */
    data class ToggleOwn(val index: Int) : TrashEntryAction

    /** Shows or hides the value of the field of the preset under [key], private by the database. */
    data class ToggleKey(val key: String) : TrashEntryAction

    /** The fab: the group to restore to is asked. */
    data object Restore : TrashEntryAction

    /** A group chosen in "restore". */
    data class GroupChosen(val id: GroupId) : TrashEntryAction

    /** The bin in the top bar: asked first - for good. */
    data object Remove : TrashEntryAction

    /** The answer of the dialog shown. */
    data object DialogConfirm : TrashEntryAction

    data object DialogDismiss : TrashEntryAction

    /** "OK" under "the file is not written": the change is made - back to the bin. */
    data object NotSavedSeen : TrashEntryAction

    /** The snackbar took the message. */
    data object MessageShown : TrashEntryAction
}

/** What the snackbar says. */
enum class TrashEntryMessage { LoadFailed, Unexpected }
