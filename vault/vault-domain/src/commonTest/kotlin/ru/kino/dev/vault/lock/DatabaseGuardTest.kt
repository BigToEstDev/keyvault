package ru.kino.dev.vault.lock

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/** A change once begun runs to the end; waiting for the guard and reading under it stay cancellable. */
@OptIn(ExperimentalCoroutinesApi::class)
class DatabaseGuardTest {

    private val guard = DatabaseGuard()
    private val steps = mutableListOf<String>()

    @Test
    fun `a change cancelled between its steps still makes the second`() = runTest {
        val between = CompletableDeferred<Unit>()
        val job = launch {
            guard.exclusiveChange {
                steps += "changed"
                between.await()
                steps += "written"
            }
        }
        runCurrent()

        job.cancel()
        between.complete(Unit)
        job.join()

        assertEquals(listOf("changed", "written"), steps)
        assertTrue(job.isCancelled)
    }

    @Test
    fun `a change cancelled while it waits for the guard is never begun`() = runTest {
        val held = CompletableDeferred<Unit>()
        val holder = launch { guard.exclusiveChange { held.await() } }
        runCurrent()
        val waiting = launch { guard.exclusiveChange { steps += "changed" } }
        runCurrent()

        waiting.cancel()
        held.complete(Unit)
        holder.join()
        waiting.join()

        assertEquals(emptyList(), steps)
    }

    @Test
    fun `the guard is free again after a cancelled change`() = runTest {
        val between = CompletableDeferred<Unit>()
        val job = launch { guard.exclusiveChange { between.await() } }
        runCurrent()
        job.cancel()
        between.complete(Unit)
        job.join()

        guard.exclusiveChange { steps += "next" }

        assertEquals(listOf("next"), steps)
    }

    @Test
    fun `a read under the guard goes with its caller`() = runTest {
        val reading = CompletableDeferred<Unit>()
        val job = launch {
            guard.exclusive {
                reading.await()
                steps += "read"
            }
        }
        runCurrent()

        job.cancel()
        job.join()

        assertEquals(emptyList(), steps)
        guard.exclusiveChange { steps += "next" }
        assertEquals(listOf("next"), steps)
    }
}
