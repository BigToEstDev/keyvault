package ru.kino.dev.mvi

import kotlinx.coroutines.Job
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.blockingIntent
import org.orbitmvi.orbit.syntax.Syntax

/**
 * An intent for what a text field types: the state is updated before the call returns, not on a coroutine
 * later.
 *
 * A text field whose value goes the asynchronous way round - action, intent, state flow, recomposition -
 * shows the old value while the keyboard sends the next edit, and the cursor and the characters jump.
 * `blockingIntent` is Orbit's answer to exactly that. Only quick code inside: no suspending calls.
 *
 * Returns a job that is already complete, so that it fits a dispatcher of actions that returns jobs.
 */
fun <S : Any, E : Any> OrbitContainerHost<S, S, E>.textIntent(transformer: suspend Syntax<S, E>.() -> Unit): Job {
    blockingIntent(transformer = transformer)
    return completed
}

private val completed: Job = Job().apply { complete() }
