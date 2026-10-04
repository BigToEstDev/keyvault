package ru.kino.dev.vault.pres.entrieslist

sealed interface EntriesListAction {

    /** Lock the database - for now only the debug button; automatic locking of `vault` comes later. */
    data object Lock : EntriesListAction
}

sealed interface EntriesListSideEffect
