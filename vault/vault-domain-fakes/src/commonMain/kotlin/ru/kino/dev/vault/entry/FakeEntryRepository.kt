package ru.kino.dev.vault.entry

import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.vault.attachment.AttachmentsEdit
import ru.kino.dev.vault.attachment.EntryAttachment
import ru.kino.dev.vault.attachment.NewAttachment
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupId

/**
 * [EntryRepository] in memory, for tests of what is built on it: the entries change the way the core
 * changes them, and every call is written to [calls] - share it to check the order against a save.
 *
 * A failure is set per method in [failures] and thrown each time that method is called, after it is
 * recorded.
 */
class FakeEntryRepository(val calls: MutableList<String> = mutableListOf()) : EntryRepository {

    /**
     * An entry the fake holds: the group it is in, its fields in order, when it was made, its attachments - their
     * bytes in [attachmentBytes].
     *
     * @param history the old versions, oldest first
     */
    data class StoredEntry(
        val groupId: GroupId,
        val fields: List<EntryField>,
        val createdAt: Long,
        val modifiedAt: Long = createdAt,
        val history: List<StoredVersion> = emptyList(),
        val attachments: List<EntryAttachment> = emptyList(),
    ) {
        // Like the core: the state before goes into the history, and the change is stamped at [now]
        fun updatedTo(fields: List<EntryField>, now: Long, attachments: List<EntryAttachment> = this.attachments) =
            copy(
                fields = fields,
                modifiedAt = now,
                attachments = attachments,
                history = history + StoredVersion(this.fields, modifiedAt, this.attachments),
            )
    }

    /** One old version of an entry: its fields, when it was saved, its attachments then. */
    data class StoredVersion(
        val fields: List<EntryField>,
        val savedAt: Long,
        val attachments: List<EntryAttachment> = emptyList(),
    )

    /**
     * The bytes of the attachments, by id - as the core keeps them, apart from the entries. A test may put bytes here
     * and link them with [StoredEntry.attachments]. Ids of the files added - `attachment-1`, ...
     */
    val attachmentBytes = mutableMapOf<String, ByteArray>()

    private var attachmentsAdded = 0

    /** Gives the entry [id] old versions saved at [savedAt], oldest first - its fields as they are now. */
    fun putHistory(id: EntryId, vararg savedAt: Long) {
        val entry = stored.getValue(id)
        stored[id] = entry.copy(history = savedAt.map { StoredVersion(entry.fields, it) })
    }

    /** The time the next change is stamped with, in seconds. */
    fun tick(): Long = ++clock

    /** Method name to the failure it throws. */
    val failures = mutableMapOf<String, Exception>()

    val stored = LinkedHashMap<EntryId, StoredEntry>()

    /** Entries moved into the recycle bin, in order. */
    val recycled = mutableListOf<EntryId>()

    private var created = 0

    /** The creation time handed to the next entry made without one, in seconds. */
    private var clock = 1_700_000_000L

    /**
     * Puts an entry into [groupId], for setting a test up. Not recorded. Without [createdAt] every entry is
     * a second younger than the one put before it.
     */
    fun add(id: String, groupId: GroupId, fields: List<EntryField>, createdAt: Long = ++clock): EntryId =
        EntryId(id).also { stored[it] = StoredEntry(groupId, fields, createdAt) }

    override suspend fun entries(dbKey: String, groupId: GroupId): List<EntrySummary> {
        call("entries")
        return stored.filterValues { it.groupId == groupId }
            .map { (id, entry) -> entry.summary(id) }
            .sortedBy { it.title }
    }

    /** By the rules of the core: title or user name, case aside, never a protected field. */
    override suspend fun search(dbKey: String, term: String): EntrySearch {
        call("search")
        val needle = term.trim()
        if (needle.isEmpty()) return EntrySearch(term, emptyList())
        val found = stored
            .filterValues { entry ->
                listOf(StandardPreset.TITLE, StandardPreset.USER_NAME).any { key ->
                    entry.fields.firstOrNull { it.key == key && !it.protected }
                        ?.value?.contains(needle, ignoreCase = true) == true
                }
            }
            .map { (id, entry) -> entry.summary(id) }
        return EntrySearch(term, found)
    }

    override suspend fun read(dbKey: String, entryId: EntryId): EntryRecord {
        call("read")
        val entry = entry(entryId)
        return EntryRecord(entry.groupId, entry.fields, EntryTimes(entry.createdAt, entry.modifiedAt), entry.attachments)
    }

    override suspend fun create(
        dbKey: String,
        groupId: GroupId,
        fields: List<EntryField>,
        attachments: List<NewAttachment>,
    ): EntryId {
        call("create")
        refuseDuplicateKeys(fields)
        refuseAttachmentNames(attachments.map { it.name })
        val id = add("entry-${++created}", groupId, fields)
        stored[id] = stored.getValue(id).copy(attachments = attachments.map(::keep))
        return id
    }

    override suspend fun update(dbKey: String, entryId: EntryId, fields: List<EntryField>, attachments: AttachmentsEdit?) {
        call("update")
        val entry = entry(entryId)
        refuseDuplicateKeys(fields)
        val after = attachments?.let { edit ->
            // As the core: kept by name, every check before anything is kept
            val kept = edit.kept.map { attachment ->
                entry.attachments.firstOrNull { it.name == attachment.name }
                    ?: throw EntryFailure.Unexpected(IllegalArgumentException("no attachment ${attachment.name}"))
            }
            refuseAttachmentNames(edit.names)
            kept + edit.added.map(::keep)
        } ?: entry.attachments
        stored[entryId] = entry.updatedTo(fields, tick(), after)
    }

    // A copy of the bytes, as the core makes its own: the caller wipes what it handed over
    private fun keep(file: NewAttachment): EntryAttachment {
        val id = "attachment-${++attachmentsAdded}"
        attachmentBytes[id] = file.bytes.copyOf()
        return EntryAttachment(id = id, name = file.name, size = file.size)
    }

    // The core refuses a repeated or empty name, and the data layer reports that as a bug of ours
    private fun refuseAttachmentNames(names: List<String>) {
        if (names.any { it.isEmpty() } || names.toSet().size != names.size) {
            throw EntryFailure.Unexpected(IllegalArgumentException("attachment names: $names"))
        }
    }

    override suspend fun delete(dbKey: String, entryId: EntryId) {
        call("delete")
        entry(entryId)
        stored.remove(entryId)
        recycled += entryId
    }

    override suspend fun move(dbKey: String, entryId: EntryId, groupId: GroupId) {
        call("move")
        stored[entryId] = entry(entryId).copy(groupId = groupId)
    }

    private fun entry(id: EntryId) = stored[id] ?: throw EntryFailure.EntryNotFound()

    private fun StoredEntry.value(key: String) = fields.firstOrNull { it.key == key }?.value.orEmpty()

    private fun StoredEntry.summary(id: EntryId) = EntrySummary(
        id = id,
        groupId = groupId,
        title = value(StandardPreset.TITLE),
        userName = value(StandardPreset.USER_NAME),
        createdAt = createdAt,
    )

    // The core refuses two fields under one key, and the data layer reports that as a bug of ours
    private fun refuseDuplicateKeys(fields: List<EntryField>) {
        if (fields.map { it.key }.toSet().size != fields.size) {
            throw EntryFailure.Unexpected(IllegalArgumentException("two fields with one key"))
        }
    }

    private fun call(method: String) {
        calls += method
        failures[method]?.let { throw it }
    }
}
