package ru.kino.dev.vault.sync

/**
 * The file of the open database copied as it is - its own password, key file, cipher (pass-docs, Step 111): saved
 * into a file the person picks, or handed to another app - a messenger - where the platform can.
 *
 * Throws [DatabaseCopyFailure] and nothing else.
 */
interface DatabaseCopyRepository {

    /**
     * The bytes of the file of the database [dbKey], as written.
     *
     * @throws DatabaseCopyFailure.Unreadable the file could not be read
     */
    suspend fun read(dbKey: String): ByteArray

    /**
     * Writes [bytes] over the file at [uri].
     *
     * @throws DatabaseCopyFailure.NotWritten the file could not be written
     */
    suspend fun write(uri: String, bytes: ByteArray)

    /** The name of the file of [dbKey] to give the copy, as the provider tells it; null when it does not. Never fails. */
    suspend fun name(dbKey: String): String?

    /** Whether the platform hands a file to another app - android does, the desktop does not. */
    fun isShareAvailable(): Boolean

    /**
     * Puts [bytes] under [name] where another app can read them, and returns the uri to hand it.
     *
     * @throws DatabaseCopyFailure.NotWritten the file could not be put there
     */
    suspend fun prepareShare(name: String, bytes: ByteArray): String
}
