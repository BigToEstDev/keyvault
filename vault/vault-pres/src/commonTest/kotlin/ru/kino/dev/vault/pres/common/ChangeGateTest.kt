package ru.kino.dev.vault.pres.common

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import ru.kino.dev.vault.changes.FakeVaultChanges
import ru.kino.dev.vault.pres.testing.DB
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ChangeGateTest {

    private val changes = FakeVaultChanges()

    @Test
    fun `what is there as the screen opens is not a change - its first reading reads it`() = runTest {
        changes.changed(DB)
        val gate = ChangeGate(changes, DB)

        assertEquals(0, startListening(gate))
    }

    @Test
    fun `a change made elsewhere is passed on, one for each`() = runTest {
        val gate = ChangeGate(changes, DB)
        val seen = listen(gate)

        changes.changed(DB)
        changes.changed(DB)

        assertEquals(2, seen.size)
    }

    @Test
    fun `a change of another database is not`() = runTest {
        val gate = ChangeGate(changes, DB)
        val seen = listen(gate)

        changes.changed("content://other")

        assertEquals(0, seen.size)
    }

    @Test
    fun `an own write is not passed on - neither while it is made nor after`() = runTest {
        val gate = ChangeGate(changes, DB)
        val seen = listen(gate)

        gate.own { changes.changed(DB) }

        assertEquals(0, seen.size)
        assertEquals(0, startListening(gate))
    }

    @Test
    fun `after an own write a change made elsewhere is passed on again`() = runTest {
        val gate = ChangeGate(changes, DB)
        val seen = listen(gate)
        gate.own { changes.changed(DB) }

        changes.changed(DB)

        assertEquals(1, seen.size)
    }

    @Test
    fun `a change read already is not passed on when listening again`() = runTest {
        val gate = ChangeGate(changes, DB)
        changes.changed(DB)

        gate.reading()

        assertEquals(0, startListening(gate))
    }

    @Test
    fun `a change made while nobody listened is passed on when listening again`() = runTest {
        val gate = ChangeGate(changes, DB)
        changes.changed(DB)

        assertEquals(1, startListening(gate))
    }

    /** Everything [gate] passes on from now, while the test runs. */
    private fun TestScope.listen(gate: ChangeGate): List<Unit> {
        val seen = mutableListOf<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { gate.elsewhere.collect { seen += it } }
        return seen
    }

    /** How many changes [gate] passes on as a screen starts to listen. */
    private fun TestScope.startListening(gate: ChangeGate): Int {
        val seen = mutableListOf<Unit>()
        launch(UnconfinedTestDispatcher(testScheduler)) { gate.elsewhere.collect { seen += it } }.cancel()
        return seen.size
    }
}
