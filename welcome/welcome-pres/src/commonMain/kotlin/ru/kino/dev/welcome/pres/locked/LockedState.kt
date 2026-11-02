package ru.kino.dev.welcome.pres.locked

import ru.kino.dev.welcome.pres.common.PasswordEntry

/**
 * @param databaseName the name of the database - and nothing else of it is shown while it is locked
 * @param read the remembered database is read - until then the screen is the shield alone, not these defaults
 * (pass-docs, Step 72, fix 4)
 */
data class LockedState(
    val databaseName: String? = null,
    val entry: PasswordEntry = PasswordEntry(),
    val read: Boolean = false,
)
