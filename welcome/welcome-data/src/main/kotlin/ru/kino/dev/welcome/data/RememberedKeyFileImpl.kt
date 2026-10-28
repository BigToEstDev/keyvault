package ru.kino.dev.welcome.data

import kotlinx.coroutines.flow.first
import ru.kino.dev.settings.signin.RememberedKeyFile
import ru.kino.dev.welcome.CurrentDatabaseRepository

/**
 * [RememberedKeyFile] on the remembered database: only its key file changes, and only when it is the database
 * the settings changed (pass-docs, Step 67). Not a way to remember a database - one that is not remembered stays so.
 */
internal class RememberedKeyFileImpl(
    private val current: CurrentDatabaseRepository,
    private val writer: CurrentDatabaseWriter,
) : RememberedKeyFile {

    override suspend fun remember(dbUri: String, keyFileUri: String?, keyFileName: String?) {
        val database = current.current.first() ?: return
        if (database.uri != dbUri) return
        writer.remember(database.copy(keyFileUri = keyFileUri, keyFileName = keyFileName))
    }
}
