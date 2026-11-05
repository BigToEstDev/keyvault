package ru.kino.dev.vault.entry

import kotlinx.coroutines.flow.first
import ru.kino.dev.core.preset.EntryPreset
import ru.kino.dev.core.preset.PresetRepository
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.vault.group.EntryId

/**
 * An entry as its screens show it: read from the core and laid over its preset - the one of one's own it
 * links to (see [presetLink]), or the standard one. A link to a preset this app does not have - deleted, or
 * made on another phone - opens with the standard preset, the fields of that one as own fields (pass-docs,
 * `vault-our.md`).
 *
 * Throws [EntryFailure] and nothing else.
 */
class EntryOpener(
    private val entries: EntryRepository,
    private val presets: PresetRepository,
) {

    suspend fun open(dbKey: String, entryId: EntryId): OpenedEntry = layOut(entries.read(dbKey, entryId))

    /**
     * [record] laid over its preset - the one its own link names. An old version links to the preset it had
     * then, which may not be the entry's now.
     */
    suspend fun layOut(record: EntryRecord): OpenedEntry =
        OpenedEntry(record.groupId, presetOf(record.fields).layout(record.fields), record.times, record.attachments)

    private suspend fun presetOf(fields: List<EntryField>): EntryPreset {
        val id = presetLink(fields) ?: return StandardPreset.preset
        return presets.presets.first().firstOrNull { it.id == id } ?: StandardPreset.preset
    }
}
