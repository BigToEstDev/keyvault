package ru.kino.dev.vault.entry

import ru.kino.dev.core.FieldProtection
import ru.kino.dev.vault.VaultDatabaseRepository
import ru.kino.dev.vault.attachment.ATTACHMENTS_MAX_PER_ENTRY
import ru.kino.dev.vault.attachment.ATTACHMENT_MAX_BYTES
import ru.kino.dev.vault.attachment.AttachmentsEdit
import ru.kino.dev.vault.attachment.NewAttachment
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
 * Attachments go with the form, in the same call (pass-docs, Step 102): the files added are held to
 * [ATTACHMENT_MAX_BYTES], the entry they are added to - to [ATTACHMENTS_MAX_PER_ENTRY], their names to one per
 * entry - the form numbers them first and stops at the limit, so a file refused here is a bug of ours. Their bytes
 * stay the caller's to wipe.
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

    /** Creates an entry holding [content] and the files [attachments] inside [groupId], saves, and returns its id. */
    suspend fun create(
        dbKey: String,
        groupId: GroupId,
        content: EntryContent,
        attachments: List<NewAttachment> = emptyList(),
    ): EntryId = guard.exclusive {
        val normalized = content.withNamesNormalized(keptNames = emptySet())
        refuseProblems(EntryValidator.problems(normalized, read = null))
        refuseAttachments(AttachmentsEdit(kept = emptyList(), added = attachments))

        val protection = protection(dbKey)
        val fields = normalized.fields().map { it.copy(protected = it.protected || protection.protects(it.key)) }
        val id = entries.create(dbKey, groupId, fields, attachments)
        save(dbKey)
        id
    }

    /**
     * Saves [content] and [attachments] over the entry and puts it into [groupId] - one action, one write; null
     * [attachments] keeps them as they are. The checks are against the entry as it is now in the database. The same
     * fields and attachments again are not a change: they are not written - the core would put a version into the
     * history on every update (pass-docs, Step 42, decision 15); the same group is not a move. Neither - nothing is
     * written at all.
     */
    suspend fun update(
        dbKey: String,
        entryId: EntryId,
        content: EntryContent,
        groupId: GroupId,
        attachments: AttachmentsEdit? = null,
    ): Unit = guard.exclusive {
        val opened = opener.open(dbKey, entryId)
        val read = opened.content
        val normalized = content.withNamesNormalized(keptNames = read.ownFields.map { it.key }.toSet())
        val edit = attachments?.takeUnless { it.keepsExactly(opened.attachments) }
        val changed = normalized.fields() != read.fields() || edit != null
        val moved = groupId != opened.groupId
        if (!changed && !moved) return@exclusive
        if (changed) refuseProblems(EntryValidator.problems(normalized, read))
        edit?.let(::refuseAttachments)

        if (changed) entries.update(dbKey, entryId, normalized.fields(), edit)
        if (moved) entries.move(dbKey, entryId, groupId)
        save(dbKey)
    }

    /** Deletes the entry - into the recycle bin, or for good when the database does not use one - and saves. */
    suspend fun delete(dbKey: String, entryId: EntryId): Unit = guard.exclusive {
        entries.delete(dbKey, entryId)
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

    // The form numbers names and refuses large files before this: one getting here is a bug of ours
    private fun refuseAttachments(edit: AttachmentsEdit) {
        val names = edit.names
        val problem = when {
            names.any { it.isEmpty() } -> "an attachment has no name"
            names.size != names.toSet().size -> "two attachments with one name"
            edit.added.any { it.size > ATTACHMENT_MAX_BYTES } -> "a file larger than an attachment may be"
            edit.addsPastLimit -> "files added past the attachments an entry may have"
            else -> return
        }
        throw EntryFailure.Unexpected(IllegalArgumentException(problem))
    }

    private suspend fun protection(dbKey: String): FieldProtection = try {
        settings.read(dbKey).protection
    } catch (e: EntrySettingsFailure) {
        throw EntryFailure.Unexpected(e)
    }

    private suspend fun save(dbKey: String) = database.saveChange(dbKey)
}
