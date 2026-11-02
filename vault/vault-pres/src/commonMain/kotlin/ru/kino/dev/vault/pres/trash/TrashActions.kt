package ru.kino.dev.vault.pres.trash

import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId

/** What the recycle bin is asked to do - done by [TrashProcessor] (pass-docs, `docs/app/mvi-arc.md`). */
interface TrashActions {

    /** The screen is shown - first, and on every return: the bin is read again. */
    fun onLoad()

    /** "Back" in the top bar, or the system one: leaves the choice first, then the screen. */
    fun onBack()

    /** The search field changed: searched after a pause in typing; blank - the whole bin again. */
    fun onQueryChange(query: String)

    /** A long press: the choice begins with this card, or it is added to it - as a tap while choosing. */
    fun onLongPress(id: EntryId)

    /** A tap: while choosing, the card is picked or let go; otherwise the entry is read (pass-docs, Step 71). */
    fun onTap(id: EntryId)

    /** The cross of the choice: nothing picked any more. */
    fun onClearChoice()

    /** Every card shown is picked. */
    fun onPickAll()

    /** "Restore" under the choice: the group to restore to is asked. */
    fun onRestore()

    /** A group chosen in "restore". */
    fun onGroupChosen(id: GroupId)

    /** "Delete" under the choice: asked first - for good. */
    fun onRemove()

    /** "Empty the recycle bin" in the menu of the top bar: asked first - for good. */
    fun onEmpty()

    /** The answer of the dialog shown. */
    fun onDialogConfirm()

    fun onDialogDismiss()

    /** The snackbar took the message. */
    fun onMessageShown()
}
