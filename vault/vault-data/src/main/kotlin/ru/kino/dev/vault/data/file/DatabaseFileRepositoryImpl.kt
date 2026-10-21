package ru.kino.dev.vault.data.file

import ru.kino.dev.core.Compression
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.DatabaseFiles
import ru.kino.dev.core.NativeCore
import ru.kino.dev.vault.file.DatabaseFileFailure
import ru.kino.dev.vault.file.DatabaseFileInfo
import ru.kino.dev.vault.file.DatabaseFileRepository
import javax.inject.Inject

/**
 * [DatabaseFileRepository] on the core - `get_db_settings` / `set_db_settings` - and the document: the name of
 * the file is the provider's, what the person sees in the file manager, not the uri the core keeps
 * (pass-docs, Step 63).
 */
internal class DatabaseFileRepositoryImpl @Inject constructor(
    private val core: NativeCore,
    private val documents: DatabaseFiles,
) : DatabaseFileRepository {

    override suspend fun read(dbKey: String): DatabaseFileInfo {
        val file = coreCall { core.databaseFile(dbKey) }
        return DatabaseFileInfo(
            name = file.name,
            fileName = documents.displayName(dbKey),
            version = file.version,
            compression = file.compression,
        )
    }

    override suspend fun change(dbKey: String, name: String, compression: Compression) {
        coreCall { core.changeDatabaseFile(dbKey, name, compression) }
    }

    private inline fun <T> coreCall(block: () -> T): T = try {
        block()
    } catch (e: CoreException) {
        throw DatabaseFileFailure.Unexpected(e)
    }
}
