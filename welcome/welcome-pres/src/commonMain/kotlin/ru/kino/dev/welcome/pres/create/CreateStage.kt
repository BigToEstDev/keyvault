package ru.kino.dev.welcome.pres.create

import ru.kino.dev.log.ErrorLog
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.navigation.VaultRootRoute
import ru.kino.dev.navigation.WelcomeGraph
import ru.kino.dev.settings.biometric.BiometricRepository
import ru.kino.dev.welcome.PickedFile
import ru.kino.dev.welcome.WelcomeDatabaseRepository
import ru.kino.dev.welcome.WelcomeFailure
import ru.kino.dev.welcome.WelcomeFilesRepository
import ru.kino.dev.welcome.pres.common.EntryProblem
import ru.kino.dev.welcome.pres.common.toProblem
import ru.kino.dev.welcome.pres.navigation.BiometricSetupRoute
import ru.kino.dev.welcome.pres.navigation.MasterPasswordRoute

/**
 * The part of the state of a creating screen that is about files: the wait, the question about writing
 * over an existing file, and what went wrong.
 *
 * @param overwrite a picked file that is not empty, waiting for the user to say whether to write over it
 * @param interrupted the process was killed while "save as" was open: what was typed is gone, the file it made is
 * deleted - said with "OK", and creating starts over (pass-docs, Step 112)
 */
data class CreateStage(
    val busy: Boolean = false,
    val overwrite: Overwrite? = null,
    val problem: EntryProblem? = null,
    val interrupted: Boolean = false,
)

/** A picked file that already has content. */
data class Overwrite(val file: PickedFile, val uri: String)

/**
 * The steps both creating screens take with files - the master password one without a key file, the key
 * file one with it: whether a picked file would be written over, and creating the database at the end.
 *
 * The screens own their state and the order of the steps; this owns the calls and where creating leads.
 */
class CreateSteps(
    private val database: WelcomeDatabaseRepository,
    private val files: WelcomeFilesRepository,
    private val biometrics: BiometricRepository,
    private val navigator: Navigator,
    private val log: ErrorLog,
) {

    /** Whether writing into [uri] would destroy something - then the screen asks first. */
    suspend fun wouldOverwrite(uri: String, file: PickedFile): Boolean = files.wouldOverwrite(uri, file)

    suspend fun writeNewKeyFile(uri: String) = files.writeNewKeyFile(uri)

    /** Deletes the file "save as" made for a creation the killed process took with it - if nothing is in it. */
    suspend fun discardAbandoned(uri: String) = files.discardEmpty(uri)

    /** Checks an existing file picked as the key file: there, and not over the size limit. */
    suspend fun checkKeyFile(uri: String) = files.checkKeyFile(uri)

    /** The name of a picked file to show, or null when the provider does not tell or cannot be asked. */
    suspend fun displayName(uri: String): String? =
        try {
            files.displayName(uri)
        } catch (failure: WelcomeFailure) {
            null
        }

    /**
     * Creates the database in [uri] with what the draft holds, and goes on.
     *
     * With strong biometrics on the phone - to offering them: the screens of typing leave the back stack,
     * the creation graph and its draft stay, for the password to be encrypted there. Without - straight to
     * `vault`, and the graph and the draft go.
     */
    suspend fun create(uri: String, password: String, keyFileUri: String?) {
        val dbKey = database.create(uri, password, keyFileUri)
        navigator.navigate(
            if (biometrics.isAvailable()) {
                MviNavEvent.NavigateTo(BiometricSetupRoute(dbKey), popUpTo = MasterPasswordRoute, inclusive = true)
            } else {
                MviNavEvent.NavigateTo(VaultRootRoute(dbKey), popUpTo = WelcomeGraph, inclusive = true)
            },
        )
    }

    /** The answer of the screen to [failure]; ours goes to the log. */
    fun problemOf(failure: WelcomeFailure, where: String): EntryProblem {
        if (failure is WelcomeFailure.Unexpected) log.unexpected(where, failure)
        return failure.toProblem()
    }
}
