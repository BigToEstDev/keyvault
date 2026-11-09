package ru.kino.dev.vault.pres.grouplist

import ru.kino.dev.vault.filter.CreationOrder
import ru.kino.dev.vault.filter.Visibility
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId

/** What the list of a group is asked to do - done by [GroupListProcessor] (pass-docs, `docs/app/mvi-arc.md`). */
interface GroupListActions {

    /** The screen is shown - the first time, or again after "back" or unlocking: the tree is read anew. */
    fun onLoad()

    fun onQueryChange(query: String)

    fun onOpenGroup(id: GroupId)

    /** A part of the path in the top bar, counted from the root. */
    fun onOpenCrumb(index: Int)

    fun onBack()

    fun onToggleFab()

    /** Closes the menu of the fab - "back" or a tap past it; never opens it, unlike [onToggleFab]. */
    fun onCloseFab()

    /** The filter button by the search field: the dialog opens with the filter in effect. */
    fun onOpenFilters()

    /** The cross of the dialog, or a tap past it: what was chosen in it is dropped. */
    fun onCloseFilters()

    fun onFilterVisibilityChange(visibility: Visibility)

    fun onFilterOrderChange(order: CreationOrder)

    /** What is chosen in the dialog takes effect and is kept on the device. */
    fun onApplyFilters()

    /** The default takes effect and is kept on the device, whatever is chosen in the dialog. */
    fun onResetFilters()

    fun onCreateGroup()

    fun onCreateEntry()

    /** The gear of the root: the settings (pass-docs, Step 52). */
    fun onOpenSettings()

    /** The bin of the root, beside the gear: the recycle bin (pass-docs, Step 70). */
    fun onOpenTrash()

    fun onOpenEntry(id: EntryId)

    fun onEditEntry(id: EntryId)

    fun onDeleteEntry(id: EntryId)

    fun onRenameGroup(id: GroupId)

    fun onDeleteGroup(id: GroupId)

    /** "Edit" in the menu of the top bar: the group shown here, not one of its cards. */
    fun onRenameCurrent()

    /** "Delete" in the menu of the top bar: the group shown here; deleted, the screen goes to its parent. */
    fun onDeleteCurrent()

    fun onDialogNameChange(name: String)

    fun onDialogConfirm()

    fun onDialogDismiss()

    /** The snackbar took the message. */
    fun onMessageShown()

    /** Lock the database now, not waiting for the timer - the lock button (pass-docs, Step 59). */
    fun onLock()

    /**
     * "Synchronize" on the bar of the root - a change waits in memory after "Cancel", the file was written elsewhere
     * (pass-docs, Step 108).
     */
    fun onSynchronize()

    /**
     * "Save" on the bar of the root - a write the file did not hold, the file may be damaged (pass-docs, Step 112).
     */
    fun onSaveAgain()
}
