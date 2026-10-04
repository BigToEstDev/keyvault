package ru.kino.dev.vault.pres.entrieslist

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.MergeSummary
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.navigation.LockedRoute
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.vault.VaultDatabaseRepository

class EntriesListProcessorTest {

    private val database = FakeVaultDatabase()
    private val log = object : ErrorLog {
        val logged = mutableListOf<Throwable>()
        override fun unexpected(where: String, error: Throwable) {
            logged += error
        }
    }

    @Test
    fun `locking locks the database for real and puts the locked screen on top`() = runTest {
        val (navigator, events) = recordNavigation()

        EntriesListProcessor(DB, database, navigator, log).testWithInternalState(this) {
            containerHost.action(EntriesListAction.Lock).join()
            cancelAndIgnoreRemainingItems()
        }

        assertEquals(listOf("lock $DB"), database.calls)
        assertEquals(listOf(MviNavEvent.NavigateTo(LockedRoute(DB))), events)
    }

    @Test
    fun `a lock that fails stays here, says so and goes to the log`() = runTest {
        database.failure = CoreException("DbKeyNotFound", "not open")
        val (navigator, events) = recordNavigation()

        EntriesListProcessor(DB, database, navigator, log).testWithInternalState(this) {
            containerHost.action(EntriesListAction.Lock)
            expectInternalState(EntriesListState(locking = true))
            expectInternalState(EntriesListState(lockFailed = true))
        }

        assertTrue(events.isEmpty())
        assertEquals(1, log.logged.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.recordNavigation(): Pair<Navigator, List<MviNavEvent>> {
        val navigator = Navigator()
        val events = mutableListOf<MviNavEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { navigator.events.collect { events += it } }
        return navigator to events
    }

    private class FakeVaultDatabase : VaultDatabaseRepository {
        val calls = mutableListOf<String>()
        var failure: CoreException? = null

        override suspend fun lock(dbKey: String) {
            calls += "lock $dbKey"
            failure?.let { throw it }
        }

        override suspend fun save(dbKey: String) = error("not here")
        override suspend fun close(dbKey: String) = error("not here")
        override suspend fun hasChangedElsewhere(dbKey: String): Boolean = error("not here")
        override suspend fun merge(dbKey: String): MergeSummary = error("not here")
    }

    private companion object {
        const val DB = "content://docs/passwords.kdbx"
    }
}
