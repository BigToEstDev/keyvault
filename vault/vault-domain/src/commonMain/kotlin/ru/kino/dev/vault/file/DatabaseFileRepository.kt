package ru.kino.dev.vault.file

import ru.kino.dev.core.Compression
import ru.kino.dev.core.KdbxVersion

/**
 * The file of the open database, as its settings show it (pass-docs, Step 63). Of `vault`: the database is open,
 * and its file is written by `vault`. Changes go through [DatabaseFileEditor], which writes the file.
 *
 * Throws [DatabaseFileFailure] and nothing else.
 */
interface DatabaseFileRepository {

    /** What the screen shows: the name of the database, its file, the format and the compression. */
    suspend fun read(dbKey: String): DatabaseFileInfo

    /** Gives the database [name] and packs it with [compression] - in memory only, the file is not written. */
    suspend fun change(dbKey: String, name: String, compression: Compression)
}

/**
 * @param name the name of the database inside the file - what KeePass and KeePassXC show
 * @param fileName the name of the file, as the provider of the document calls it; null when it does not say
 * @param version the version of KDBX the file was read as; 4.1 once saved
 */
data class DatabaseFileInfo(
    val name: String,
    val fileName: String?,
    val version: KdbxVersion,
    val compression: Compression,
)

/** Every way reading or changing the file of the database can fail, as the settings answer it. */
sealed class DatabaseFileFailure(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** The name is empty - a database always has one. Said in the dialog. */
    class EmptyName : DatabaseFileFailure("The name of a database is not empty")

    /** The change is made in the database, but writing the file failed: it waits in memory for the next save. */
    class NotSaved(cause: Throwable? = null) : DatabaseFileFailure("The change is made but not written to the file", cause)

    /** The core refused - the database closed meanwhile, or a bug. The cause to the log. */
    class Unexpected(cause: Throwable? = null) : DatabaseFileFailure("Something went wrong", cause)
}
