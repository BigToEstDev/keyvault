package ru.kino.dev.welcome.pres.create

import ru.kino.dev.log.ErrorLog
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.welcome.PickedFile
import ru.kino.dev.welcome.WelcomeDatabaseRepository
import ru.kino.dev.welcome.WelcomeFailure
import ru.kino.dev.welcome.WelcomeFilesRepository
import ru.kino.dev.welcome.pres.common.EntryProblem
import ru.kino.dev.welcome.pres.common.toProblem
import ru.kino.dev.welcome.pres.navigation.BiometricSetupRoute
import ru.kino.dev.welcome.pres.navigation.CreateGraph
import javax.inject.Inject

/**
 * The part of the state of a creating screen that is about files: the wait, the question about writing
 * over an existing file, and what went wrong.
 *
 * @param overwrite a picked file that is not empty, waiting for the user to say whether to write over it
 */
data class CreateStage(
    val busy: Boolean = false,
    val overwrite: Overwrite? = null,
    val problem: EntryProblem? = null,
)

/** A picked file that already has content. */
data class Overwrite(val file: PickedFile, val uri: String)

/**
 * The steps both creating screens take with files - the master password one without a key file, the key
 * file one with it: whether a picked file would be written over, and creating the database at the end.
 *
 * The screens own their state and the order of the steps; this owns the calls and where creating leads.
 */
class CreateSteps @Inject constructor(
    private val database: WelcomeDatabaseRepository,
    private val files: WelcomeFilesRepository,
    private val navigator: Navigator,
    private val log: ErrorLog,
) {

    /** Whether writing into [uri] would destroy something - then the screen asks first. */
    suspend fun wouldOverwrite(uri: String, file: PickedFile): Boolean = files.wouldOverwrite(uri, file)

    suspend fun writeNewKeyFile(uri: String) = files.writeNewKeyFile(uri)

    /**
     * Creates the database in [uri] with what the draft holds, and goes on to biometrics. The creation
     * graph goes from the back stack - and the draft with it: the database is open, the password is not
     * needed again.
     */
    suspend fun create(uri: String, password: String, keyFileUri: String?) {
        val dbKey = database.create(uri, password, keyFileUri)
        navigator.navigate(
            MviNavEvent.NavigateTo(BiometricSetupRoute(dbKey), popUpTo = CreateGraph, inclusive = true),
        )
    }

    /** The answer of the screen to [failure]; ours goes to the log. */
    fun problemOf(failure: WelcomeFailure, where: String): EntryProblem {
        if (failure is WelcomeFailure.Unexpected) log.unexpected(where, failure)
        return failure.toProblem()
    }
}
