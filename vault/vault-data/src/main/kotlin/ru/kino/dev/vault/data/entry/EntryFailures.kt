package ru.kino.dev.vault.data.entry

import ru.kino.dev.core.CoreException
import ru.kino.dev.vault.entry.EntryFailure
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs a call of the entry repository so that it fails with an [EntryFailure] and nothing else.
 *
 * `NotFound` is the core's answer for an id that is not there - which one depends on the call, so the
 * caller names it in [notFound]: the entry for most calls, the group when an entry is created in one.
 * `DuplicateFieldKey` is [EntryFailure.Unexpected]: [ru.kino.dev.vault.entry.EntryEditor] checks the names
 * first, so the core refusing one is a bug of ours. Everything else (`DbKeyNotFound`, the bridge's own
 * kinds, a bug) is [EntryFailure.Unexpected] too. Cancellation passes untouched.
 */
internal suspend fun <T> entryCall(
    notFound: (CoreException) -> EntryFailure = { EntryFailure.EntryNotFound(it) },
    block: suspend () -> T,
): T =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: EntryFailure) {
        throw e
    } catch (e: CoreException) {
        throw if (e.kind == NOT_FOUND) notFound(e) else EntryFailure.Unexpected(e)
    } catch (e: Exception) {
        throw EntryFailure.Unexpected(e)
    }

private const val NOT_FOUND = "NotFound"
