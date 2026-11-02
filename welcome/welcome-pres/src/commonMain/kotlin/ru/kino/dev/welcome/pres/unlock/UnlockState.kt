package ru.kino.dev.welcome.pres.unlock

import ru.kino.dev.welcome.pres.common.PasswordEntry

/**
 * @param databaseUri the database to open; null until it is known, and when nothing is remembered
 * @param databaseName the name from inside the database, known once it was opened before
 * @param fileName the name of the file, shown when the database has not been opened yet
 * @param read the database to show is read - until then the screen is the shield alone, not these defaults
 * (pass-docs, Step 72, fix 4)
 */
data class UnlockState(
    val databaseUri: String? = null,
    val databaseName: String? = null,
    val fileName: String? = null,
    val entry: PasswordEntry = PasswordEntry(),
    val read: Boolean = false,
) {
    /** The title of the screen: the name from inside the database, else the file name. */
    val title: String? = databaseName ?: fileName
}
