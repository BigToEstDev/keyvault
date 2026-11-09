package ru.kino.dev.welcome.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import ru.kino.dev.core.CredentialLimits
import ru.kino.dev.core.DatabaseFiles
import ru.kino.dev.core.NativeCore
import ru.kino.dev.welcome.PickedFile
import ru.kino.dev.welcome.WelcomeFailure
import ru.kino.dev.welcome.WelcomeFilesRepository

/** [WelcomeFilesRepository] over the picked documents and the key file generator of the core. */
internal class WelcomeFilesRepositoryImpl(
    private val core: NativeCore,
    private val files: DatabaseFiles,
) : WelcomeFilesRepository {

    override suspend fun displayName(uri: String): String? = welcomeCall { files.displayName(uri) }

    override suspend fun isReachable(uri: String): Boolean = welcomeCall { files.exists(uri) }

    override suspend fun wouldOverwrite(uri: String, file: PickedFile): Boolean = welcomeCall {
        !accessing(file) { files.isEmpty(uri) }
    }

    override suspend fun checkKeyFile(uri: String) = welcomeCall {
        if (!files.exists(uri)) throw WelcomeFailure.FileUnreachable(PickedFile.KeyFile)
        // An unknown size passes: creating reads at most the limit and refuses then
        val size = files.size(uri)
        if (size != null && CredentialLimits.isKeyFileTooLarge(size)) throw WelcomeFailure.KeyFileTooLarge()
    }

    override suspend fun writeNewKeyFile(uri: String) = welcomeCall {
        val content = core.generateKeyFile()
        try {
            // A key file cut short by cancelling is a key nobody can reproduce
            withContext(NonCancellable) {
                accessing(PickedFile.KeyFile) { files.writeReplacing(uri, content) }
            }
        } finally {
            // The content is a secret on par with the password; from here on it lives in the file only
            content.fill(0)
        }
    }

    override suspend fun discardEmpty(uri: String) {
        try {
            if (files.isEmpty(uri)) files.delete(uri)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Out of reach: the file stays, and there is nothing in it to lose
        }
    }
}
