package ru.kino.dev.preset

import kotlinx.serialization.Serializable
import ru.kino.dev.core.preset.CustomPreset
import ru.kino.dev.core.preset.EntryPreset
import ru.kino.dev.core.preset.PresetId

/**
 * The presets of one's own as the store keeps them - one json string. Only what the person sets is kept:
 * the rest of a preset is fixed by [CustomPreset] and built back by it.
 *
 * [version] grows when the shape changes in a way an older app cannot read. A field added with a default is not
 * such a change: an older app skips it and reads the rest ([PresetDto.protectedKeys], [PresetDto.fieldTypes],
 * pass-docs, Step 75).
 */
@Serializable
internal data class PresetsDto(
    val version: Int,
    val presets: List<PresetDto>,
)

/**
 * @param cards the names of the fields of each card, as [CustomPreset.of] takes them
 * @param protectedKeys the names of those fields that are protected; none in what was kept before Step 75
 * @param fieldTypes the types of the fields, by name - text is none; none in what was kept before Step 75, item 3
 */
@Serializable
internal data class PresetDto(
    val id: String,
    val name: String,
    val cards: List<List<String>>,
    val protectedKeys: List<String> = emptyList(),
    val fieldTypes: Map<String, String> = emptyMap(),
)

internal fun EntryPreset.toDto(): PresetDto = PresetDto(
    id = requireNotNull(id).value,
    name = requireNotNull(name),
    cards = CustomPreset.cards(this),
    protectedKeys = CustomPreset.protectedKeys(this).toList(),
    fieldTypes = CustomPreset.fieldTypes(this),
)

internal fun PresetDto.toPreset(): EntryPreset =
    CustomPreset.of(PresetId(id), name, cards, protectedKeys.toSet(), fieldTypes)
