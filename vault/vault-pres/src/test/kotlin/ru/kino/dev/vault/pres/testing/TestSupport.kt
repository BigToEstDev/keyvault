package ru.kino.dev.vault.pres.testing

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.MergeSummary
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.vault.VaultDatabaseRepository

/*
 * What the tests of the processors of `vault` share: the database they save into, the log, the navigation.
 */

const val DB = "content://docs/passwords.kdbx"

/** A navigator, and every event sent through it, in order. */
@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.recordNavigation(): Pair<Navigator, List<MviNavEvent>> {
    val navigator = Navigator()
    val events = mutableListOf<MviNavEvent>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { navigator.events.collect { events += it } }
    return navigator to events
}

/** The open database: what was asked of it, and a failure to throw instead. */
class FakeVaultDatabase : VaultDatabaseRepository {
    val calls = mutableListOf<String>()
    var saveFailure: Exception? = null
    var lockFailure: CoreException? = null

    override suspend fun save(dbKey: String) {
        calls += "save"
        saveFailure?.let { throw it }
    }

    override suspend fun lock(dbKey: String) {
        calls += "lock"
        lockFailure?.let { throw it }
    }

    override suspend fun close(dbKey: String) = error("not here")
    override suspend fun hasChangedElsewhere(dbKey: String): Boolean = error("not here")
    override suspend fun merge(dbKey: String): MergeSummary = error("not here")
}

class RecordingLog : ErrorLog {
    val logged = mutableListOf<Throwable>()

    override fun unexpected(where: String, error: Throwable) {
        logged += error
    }
}
