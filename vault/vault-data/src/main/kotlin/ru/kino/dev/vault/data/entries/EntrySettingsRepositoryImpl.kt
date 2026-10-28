package ru.kino.dev.vault.data.entries

import ru.kino.dev.core.CoreException
import ru.kino.dev.core.DatabaseEntries
import ru.kino.dev.core.NativeCore
import ru.kino.dev.vault.entries.EntrySettingsFailure
import ru.kino.dev.vault.entries.EntrySettingsRepository

/** [EntrySettingsRepository] on the core - `get_db_settings` / `set_db_settings` (pass-docs, Step 65). */
internal class EntrySettingsRepositoryImpl(
    private val core: NativeCore,
) : EntrySettingsRepository {

    override suspend fun read(dbKey: String): DatabaseEntries = coreCall { core.databaseEntries(dbKey) }

    override suspend fun change(dbKey: String, entries: DatabaseEntries) {
        coreCall { core.changeDatabaseEntries(dbKey, entries) }
    }

    private inline fun <T> coreCall(block: () -> T): T = try {
        block()
    } catch (e: CoreException) {
        throw EntrySettingsFailure.Unexpected(e)
    }
}
