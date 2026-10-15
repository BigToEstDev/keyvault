package ru.kino.dev.vault.entry

import ru.kino.dev.core.preset.EntryPreset
import ru.kino.dev.core.preset.PresetField

/**
 * What an entry holds, as the screens of an entry show and edit it: the fields of its [preset] in the
 * preset's order, then its own fields.
 *
 * Made by [EntryPreset.layout] from what the core gives, or by [EntryPreset.blank] for a new entry; a screen
 * changes values and own fields and hands it back.
 */
data class EntryContent(
    val preset: EntryPreset,
    /** One per field of the preset, in its order; empty when the entry lacks it */
    val presetFields: List<EntryField>,
    /** The rest of the entry, in the order of the file; a new one goes last */
    val ownFields: List<EntryField>,
) {

    init {
        require(presetFields.map { it.key } == preset.fields.map { it.key }) { "preset fields out of their preset" }
    }

    /** The value of the field of the preset under [key]. */
    fun value(key: String): String = presetFields.first { it.key == key }.value

    /** The same content with [value] in the field of the preset under [key]. */
    fun withValue(key: String, value: String): EntryContent =
        copy(presetFields = presetFields.map { if (it.key == key) it.copy(value = value) else it })

    /**
     * The strings to write, in the order of the form: the preset, then own fields. A field of ours that is
     * empty is left out (see [PresetField.writtenWhenEmpty]).
     */
    fun fields(): List<EntryField> {
        val written = presetFields.filterIndexed { index, field ->
            field.value.isNotEmpty() || preset.fields[index].writtenWhenEmpty
        }
        return written + ownFields
    }
}
