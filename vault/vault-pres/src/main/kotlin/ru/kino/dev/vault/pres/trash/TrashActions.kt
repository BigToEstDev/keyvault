package ru.kino.dev.vault.pres.trash

sealed interface TrashAction {

    /** The screen is shown - first, and on every return: the bin is read again. */
    data object Load : TrashAction

    data object Back : TrashAction

    /** The search field changed: searched after a pause in typing; blank - the whole bin again. */
    data class QueryChanged(val query: String) : TrashAction

    /** The snackbar took the message. */
    data object MessageShown : TrashAction
}

/** Why the list is empty - what the empty screen says. */
enum class TrashEmpty {
    /** Nothing in the bin. */
    Bin,

    /** The search found nothing. */
    NothingFound,
}

/** What the snackbar says. */
enum class TrashMessage {
    /** The bin could not be read. */
    LoadFailed,

    /** The search failed. */
    SearchFailed,
}
