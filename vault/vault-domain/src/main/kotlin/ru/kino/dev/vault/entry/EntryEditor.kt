package ru.kino.dev.vault.entry

import ru.kino.dev.vault.VaultDatabaseRepository
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId
import kotlin.coroutines.cancellation.CancellationException

/**
 * Creating, changing and deleting entries, the way a person does it: one action, one write of the file.
 *
 * Each change is checked by [EntryValidator], made in the core and saved - the check lives here and not on
 * the screen alone: a screen can be wrong, the database cannot.
 *
 * Throws [EntryFailure] and nothing else.
 */
class EntryEditor(
    private val entries: EntryRepository,
    private val database: VaultDatabaseRepository,
) {

    /** Creates an entry holding [content] inside [groupId], saves, and returns its id. */
    suspend fun create(dbKey: String, groupId: GroupId, content: EntryContent): EntryId {
        val normalized = content.withNamesNormalized(keptNames = emptySet())
        refuseProblems(EntryValidator.problems(normalized, read = null))

        val id = entries.create(dbKey, groupId, normalized.fields())
        save(dbKey)
        return id
    }

    /**
     * Saves [content] over the entry. The checks are against the entry as it is now in the database. The
     * same fields again are not a change: nothing is made and nothing is written - the core would put a
     * version into the history on every update (pass-docs, Step 42, decision 15).
     */
    suspend fun update(dbKey: String, entryId: EntryId, content: EntryContent) {
        val read = entries.content(dbKey, entryId)
        val normalized = content.withNamesNormalized(keptNames = read.ownFields.map { it.key }.toSet())
        if (normalized.fields() == read.fields()) return
        refuseProblems(EntryValidator.problems(normalized, read))

        entries.update(dbKey, entryId, normalized.fields())
        save(dbKey)
    }

    /** Moves the entry into the recycle bin and saves. */
    suspend fun delete(dbKey: String, entryId: EntryId) {
        entries.moveToRecycleBin(dbKey, entryId)
        save(dbKey)
    }

    /**
     * Names of own fields written here go without the spaces around them; a name as it was read stays as
     * another program spelled it.
     */
    private fun EntryContent.withNamesNormalized(keptNames: Set<String>) = copy(
        ownFields = ownFields.map {
            if (it.key in keptNames) it else it.copy(key = EntryValidator.normalizeName(it.key))
        },
    )

    private fun refuseProblems(problems: List<FieldProblem>) {
        if (problems.isNotEmpty()) throw EntryFailure.InvalidFields(problems)
    }

    private suspend fun save(dbKey: String) {
        try {
            database.save(dbKey)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw EntryFailure.NotSaved(e)
        }
    }
}
