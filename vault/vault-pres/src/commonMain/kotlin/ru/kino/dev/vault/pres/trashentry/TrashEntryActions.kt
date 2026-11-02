package ru.kino.dev.vault.pres.trashentry

import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.pres.common.EntryReadFieldsActions

/**
 * What an entry of the recycle bin is asked to do - done by [TrashEntryProcessor]
 * (pass-docs, `docs/app/mvi-arc.md`).
 */
interface TrashEntryActions : EntryReadFieldsActions {

    /** The screen is shown: the deleted entry is read. */
    fun onLoad()

    fun onBack()

    /** The fab: the group to restore to is asked. */
    fun onRestore()

    /** A group chosen in "restore". */
    fun onGroupChosen(id: GroupId)

    /** The bin in the top bar: asked first - for good. */
    fun onRemove()

    /** The answer of the dialog shown. */
    fun onDialogConfirm()

    fun onDialogDismiss()

    /** "OK" under "the file is not written": the change is made - back to the bin. */
    fun onNotSavedSeen()

    /** The snackbar took the message. */
    fun onMessageShown()
}
