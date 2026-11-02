package ru.kino.dev.vault.pres.entryversion

import ru.kino.dev.vault.pres.common.EntryReadFieldsActions

/**
 * What an old version of an entry is asked to do - done by [EntryVersionProcessor]
 * (pass-docs, `docs/app/mvi-arc.md`).
 */
interface EntryVersionActions : EntryReadFieldsActions {

    /** The screen is shown: the version is read. */
    fun onLoad()

    fun onBack()

    /** The fab with the clock: asks first. */
    fun onRestore()

    fun onDialogConfirm()

    fun onDialogDismiss()

    /** The snackbar took the message. */
    fun onMessageShown()
}
