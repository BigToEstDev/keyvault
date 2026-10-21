package ru.kino.dev.vault.entry

import ru.kino.dev.core.FieldProtection
import ru.kino.dev.vault.VaultDatabaseRepository
import ru.kino.dev.vault.entries.EntrySettingsFailure
import ru.kino.dev.vault.entries.EntrySettingsRepository
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.lock.DatabaseGuard

/**
 * Creating, changing and deleting entries, the way a person does it: one action, one write of the file.
 *
 * Each change is checked by [EntryValidator], made in the core and saved - the check lives here and not on
 * the screen alone: a screen can be wrong, the database cannot.
 *
 * Each change runs inside [guard], so a lock waits for its write to end.
 *
 * A new entry is private where the database says so - a field of [FieldProtection] is protected whatever the
 * preset says (pass-docs, Step 65); an entry changed keeps the protection its fields have.
 *
 * Throws [EntryFailure] and nothing else.
 */
class EntryEditor(
    private val entries: EntryRepository,
    private val opener: EntryOpener,
    private val settings: EntrySettingsRepository,
    private val database: VaultDatabaseRepository,
    private val guard: DatabaseGuard = DatabaseGuard(),
) {

    /** Creates an entry holding [content] inside [groupId], saves, and returns its id. */
    suspend fun create(dbKey: String, groupId: GroupId, content: EntryContent): EntryId = guard.exclusive {
        val normalized = content.withNamesNormalized(keptNames = emptySet())
        refuseProblems(EntryValidator.problems(normalized, read = null))

        val protection = protection(dbKey)
        val fields = normalized.fields().map { it.copy(protected = it.protected || protection.protects(it.key)) }
        val id = entries.create(dbKey, groupId, fields)
        save(dbKey)
        id
    }

    /**
     * Saves [content] over the entry and puts it into [groupId] - one action, one write. The checks are
     * against the entry as it is now in the database. The same fields again are not a change: they are not
     * written - the core would put a version into the history on every update (pass-docs, Step 42, decision
     * 15); the same group is not a move. Neither - nothing is written at all.
     */
    suspend fun update(
        dbKey: String,
        entryId: EntryId,
        content: EntryContent,
        groupId: GroupId,
    ): Unit = guard.exclusive {
        val opened = opener.open(dbKey, entryId)
        val read = opened.content
        val normalized = content.withNamesNormalized(keptNames = read.ownFields.map { it.key }.toSet())
        val changed = normalized.fields() != read.fields()
        val moved = groupId != opened.groupId
        if (!changed && !moved) return@exclusive
        if (changed) refuseProblems(EntryValidator.problems(normalized, read))

        if (changed) entries.update(dbKey, entryId, normalized.fields())
        if (moved) entries.move(dbKey, entryId, groupId)
        save(dbKey)
    }

    /** Moves the entry into the recycle bin and saves. */
    suspend fun delete(dbKey: String, entryId: EntryId): Unit = guard.exclusive {
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

    private suspend fun protection(dbKey: String): FieldProtection = try {
        settings.read(dbKey).protection
    } catch (e: EntrySettingsFailure) {
        throw EntryFailure.Unexpected(e)
    }

    private suspend fun save(dbKey: String) = database.saveChange(dbKey)
}
