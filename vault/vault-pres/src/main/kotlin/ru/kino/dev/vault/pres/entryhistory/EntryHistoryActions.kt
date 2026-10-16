package ru.kino.dev.vault.pres.entryhistory

sealed interface EntryHistoryAction {

    /** The screen is shown - the first time, or again: the history is read anew. */
    data object Load : EntryHistoryAction

    data object Back : EntryHistoryAction

    /** The snackbar took the message. */
    data object MessageShown : EntryHistoryAction
}

/** What the snackbar says. */
enum class EntryHistoryMessage { LoadFailed }
