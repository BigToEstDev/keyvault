package ru.kino.dev.vault.data.trash

import ru.kino.dev.core.CoreException
import ru.kino.dev.core.NativeCore
import ru.kino.dev.core.TrashEntryData
import ru.kino.dev.vault.data.entry.toSummary
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.trash.TrashEntry
import ru.kino.dev.vault.trash.TrashFailure
import ru.kino.dev.vault.trash.TrashRepository
import ru.kino.dev.vault.trash.TrashSearch
import kotlin.coroutines.cancellation.CancellationException

/** [TrashRepository] over the native core (pass-docs, Step 69 / 70). Nothing is cached. */
internal class TrashRepositoryImpl(
    private val core: NativeCore,
) : TrashRepository {

    override suspend fun enabled(dbKey: String): Boolean = trashCall { core.recycleBinEnabled(dbKey) }

    override suspend fun setEnabled(dbKey: String, enabled: Boolean) = trashCall {
        core.changeRecycleBinEnabled(dbKey, enabled)
    }

    override suspend fun entries(dbKey: String): List<TrashEntry> = trashCall {
        core.trashEntries(dbKey).map { it.toTrashEntry() }
    }

    override suspend fun search(dbKey: String, term: String): TrashSearch = trashCall {
        val found = core.searchTrash(dbKey, term)
        TrashSearch(term = found.term, entries = found.entries.map { it.toTrashEntry() })
    }

    override suspend fun restore(dbKey: String, entryIds: List<EntryId>, groupId: GroupId) = trashCall {
        core.restoreEntries(dbKey, entryIds.map { it.value }, groupId.value)
    }

    override suspend fun remove(dbKey: String, entryIds: List<EntryId>) = trashCall {
        core.removeEntries(dbKey, entryIds.map { it.value })
    }

    override suspend fun empty(dbKey: String) = trashCall { core.emptyTrash(dbKey) }

    private fun TrashEntryData.toTrashEntry() = TrashEntry(
        entry = entry.toSummary(),
        previousGroupId = previousGroupId?.let(::GroupId),
        deletedAt = deletedAt,
    )

    // What the core refuses before changing anything because the list was stale - an entry gone from the bin, a
    // group gone - is told apart: the screen reads the bin again
    private suspend fun <T> trashCall(block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: CoreException) {
        throw if (e.kind in STALE) TrashFailure.Stale(e) else TrashFailure.Unexpected(e)
    } catch (e: Exception) {
        throw TrashFailure.Unexpected(e)
    }

    private companion object {
        val STALE = setOf("NotFound", "DataError")
    }
}
