package ru.kino.dev.vault.data.group

import ru.kino.dev.core.CoreException
import ru.kino.dev.vault.group.GroupFailure
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs a call of the group repository so that it fails with a [GroupFailure] and nothing else.
 *
 * `NotFound` is the core's answer for a group that is not there - asked for by id, or named as a parent;
 * everything else (`DbKeyNotFound`, the bridge's own kinds, a bug) is [GroupFailure.Unexpected].
 * Cancellation passes untouched - it is not a failure of the call.
 */
internal suspend fun <T> groupCall(block: suspend () -> T): T =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: GroupFailure) {
        throw e
    } catch (e: CoreException) {
        throw if (e.kind == NOT_FOUND) GroupFailure.GroupNotFound(e) else GroupFailure.Unexpected(e)
    } catch (e: Exception) {
        throw GroupFailure.Unexpected(e)
    }

private const val NOT_FOUND = "NotFound"
