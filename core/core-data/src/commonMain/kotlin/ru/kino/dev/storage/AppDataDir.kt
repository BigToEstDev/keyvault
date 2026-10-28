package ru.kino.dev.storage

/**
 * The private directory of the app: the settings and the remembered database live under it, never a database
 * itself - those stay where the user put them. `filesDir` on android, so the files kept before the move to
 * Compose Multiplatform stay where they were; the data directory of the user on the desktop (pass-docs, Step 86).
 */
interface AppDataDir {

    /** The absolute path of the directory; there by the time it is read. */
    val path: String
}
