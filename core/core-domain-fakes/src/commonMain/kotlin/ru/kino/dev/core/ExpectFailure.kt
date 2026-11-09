package ru.kino.dev.core

/**
 * Runs [block] and returns the failure of type [T] it ended with.
 *
 * Inline, so the block may call suspend functions from inside `runTest` - junit's `assertThrows` takes a
 * plain lambda and would need a second, blocking coroutine for that.
 */
inline fun <reified T : Throwable> expectFailure(block: () -> Unit): T {
    try {
        block()
    } catch (e: Throwable) {
        if (e is T) return e
        throw AssertionError("expected ${T::class.simpleName}, got $e", e)
    }
    throw AssertionError("expected ${T::class.simpleName}, nothing was thrown")
}

/**
 * Checks that [failure] was caused by [cause] - directly or further down the chain. Not `failure.cause` alone: a
 * failure that comes out of `withContext` (a change under `DatabaseGuard.exclusiveChange`) is copied by the stack
 * trace recovery of kotlinx.coroutines when the tests run with assertions on, and the copy holds the original as
 * its cause (pass-docs, Step 114).
 */
fun assertCausedBy(cause: Throwable, failure: Throwable) {
    if (generateSequence(failure) { it.cause }.none { it === cause }) {
        throw AssertionError("expected $failure to be caused by $cause", failure)
    }
}
