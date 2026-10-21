package ru.kino.dev.vault.file

import ru.kino.dev.core.Compression
import ru.kino.dev.core.KdbxVersion

/**
 * The file of the database in memory: what [read] answers, changed by [change]. [failure], when set, is thrown by
 * every call instead.
 */
class FakeDatabaseFileRepository(
    var info: DatabaseFileInfo = DatabaseFileInfo(
        name = "Passwords",
        fileName = "passwords.kdbx",
        version = KdbxVersion.V41,
        compression = Compression.GZip,
    ),
) : DatabaseFileRepository {

    var failure: DatabaseFileFailure? = null

    /** Every change asked for, as `name/compression`. */
    val changes = mutableListOf<String>()

    override suspend fun read(dbKey: String): DatabaseFileInfo {
        failure?.let { throw it }
        return info
    }

    override suspend fun change(dbKey: String, name: String, compression: Compression) {
        failure?.let { throw it }
        changes += "$name/$compression"
        info = info.copy(name = name, compression = compression)
    }
}
