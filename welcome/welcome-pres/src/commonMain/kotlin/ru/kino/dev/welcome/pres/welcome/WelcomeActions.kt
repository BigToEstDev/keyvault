package ru.kino.dev.welcome.pres.welcome

/**
 * What the welcome screen is asked to do - done by [WelcomeProcessor]: the layout's, and the file the picker hands
 * back (pass-docs `docs/app/mvi-arc.md`).
 */
interface WelcomeActions {

    /** "Open a database": the picker of an existing file. */
    fun onOpen()

    /** "Create database": the master password comes first. */
    fun onCreate()

    fun onDatabasePicked(uri: String)
}
