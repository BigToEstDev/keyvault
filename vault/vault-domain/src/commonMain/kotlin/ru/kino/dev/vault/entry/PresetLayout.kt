package ru.kino.dev.vault.entry

import ru.kino.dev.core.preset.EntryPreset
import ru.kino.dev.core.preset.PresetField
import ru.kino.dev.core.preset.PresetId
import ru.kino.dev.core.preset.PresetLink

/**
 * The fields of an entry laid over this preset. A field of the preset is found by its key, exactly as the
 * file spells it; one the entry lacks is empty. Everything else is an own field, in the order of the file -
 * a second field under a key of the preset too: an entry of another program may hold one, and the person
 * renames or deletes it.
 *
 * The link to a preset (see [PresetLink]) is no field of the content: the screens never show it, and
 * [EntryContent.fields] writes it back from the preset - or not at all, when the entry is laid over the
 * standard one.
 */
fun EntryPreset.layout(fields: List<EntryField>): EntryContent {
    val rest = fields.filterNot { it.key == PresetLink.KEY }.toMutableList()
    val presetFields = this.fields.map { presetField ->
        val index = rest.indexOfFirst { it.key == presetField.key }
        if (index >= 0) rest.removeAt(index) else presetField.empty()
    }
    return EntryContent(this, presetFields, rest)
}

/** The content of a new entry: every field of the preset empty, protected and typed as the preset says. */
fun EntryPreset.blank(): EntryContent = EntryContent(this, fields.map { it.empty() }, emptyList())

/** The preset the entry of [fields] links to, or null when it links to none - an entry of the standard one. */
fun presetLink(fields: List<EntryField>): PresetId? =
    fields.firstOrNull { it.key == PresetLink.KEY }?.value?.takeIf { it.isNotEmpty() }?.let(::PresetId)

internal fun PresetField.empty() = EntryField(key, value = "", protected = protected, type = type)
