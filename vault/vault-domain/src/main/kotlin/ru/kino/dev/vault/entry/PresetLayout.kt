package ru.kino.dev.vault.entry

import ru.kino.dev.core.preset.EntryPreset
import ru.kino.dev.core.preset.PresetField

/**
 * The fields of an entry laid over this preset. A field of the preset is found by its key, exactly as the
 * file spells it; one the entry lacks is empty. Everything else is an own field, in the order of the file -
 * a second field under a key of the preset too: an entry of another program may hold one, and the person
 * renames or deletes it.
 */
fun EntryPreset.layout(fields: List<EntryField>): EntryContent {
    val rest = fields.toMutableList()
    val presetFields = this.fields.map { presetField ->
        val index = rest.indexOfFirst { it.key == presetField.key }
        if (index >= 0) rest.removeAt(index) else presetField.empty()
    }
    return EntryContent(this, presetFields, rest)
}

/** The content of a new entry: every field of the preset empty, protected as the preset says. */
fun EntryPreset.blank(): EntryContent = EntryContent(this, fields.map { it.empty() }, emptyList())

internal fun PresetField.empty() = EntryField(key, value = "", protected = protected)
