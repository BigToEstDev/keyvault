package ru.kino.dev.vault.data.changes

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class VaultChangesImplTest {

    private val changes = VaultChangesImpl()

    @Test
    fun `a database not changed yet is at its first version`() = runTest {
        assertEquals(0, changes.current(DB))
        assertEquals(0, changes.versions(DB).first())
    }

    @Test
    fun `every change is one version more`() = runTest {
        changes.changed(DB)
        changes.changed(DB)

        assertEquals(2, changes.current(DB))
    }

    @Test
    fun `the versions of one database do not move with another`() = runTest {
        changes.changed(OTHER)

        assertEquals(0, changes.current(DB))
        assertEquals(1, changes.current(OTHER))
    }

    @Test
    fun `a listener gets the current version, then each new one`() = runTest {
        val seen = mutableListOf<Long>()
        val listening = launch(UnconfinedTestDispatcher(testScheduler)) { changes.versions(DB).take(3).toList(seen) }

        changes.changed(OTHER)
        changes.changed(DB)
        changes.changed(DB)
        listening.join()

        assertEquals(listOf(0L, 1L, 2L), seen)
    }

    private companion object {
        const val DB = "content://db"
        const val OTHER = "content://other"
    }
}
