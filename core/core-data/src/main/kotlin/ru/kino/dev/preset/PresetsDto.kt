package ru.kino.dev.preset

import kotlinx.serialization.Serializable
import ru.kino.dev.core.preset.CustomPreset
import ru.kino.dev.core.preset.EntryPreset
import ru.kino.dev.core.preset.PresetId

/**
 * The presets of one's own as the store keeps them - one json string. Only what the person sets is kept:
 * the rest of a preset is fixed by [CustomPreset] and built back by it.
 *
 * [version] grows when the shape changes in a way an older app cannot read.
 */
@Serializable
internal data class PresetsDto(
    val version: Int,
    val presets: List<PresetDto>,
)

/** @param cards the names of the fields of each card, as [CustomPreset.of] takes them */
@Serializable
internal data class PresetDto(
    val id: String,
    val name: String,
    val cards: List<List<String>>,
)

internal fun EntryPreset.toDto(): PresetDto =
    PresetDto(id = requireNotNull(id).value, name = requireNotNull(name), cards = CustomPreset.cards(this))

internal fun PresetDto.toPreset(): EntryPreset = CustomPreset.of(PresetId(id), name, cards)
