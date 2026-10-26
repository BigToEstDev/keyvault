package ru.kino.dev.core.preset

import kotlinx.coroutines.flow.first
import java.util.UUID

/**
 * Creating, changing and deleting presets of one's own, the way a person does it. Each change is checked by
 * [PresetValidator] here and not on the screen alone: a screen can be wrong, the store cannot.
 *
 * A preset is given as the person sets it - its name, the names of the fields of each card, the names of those
 * that are protected and the types of the fields, as [CustomPreset.of] takes them. Names go without the spaces around them; a card
 * the person added and left empty is dropped - it would show nothing.
 *
 * The problems it refuses with point at the cards as given, the empty ones counted. `standardName` is what
 * the screens call the standard preset - see [PresetValidator.nameProblem].
 *
 * Throws [PresetFailure] and nothing else.
 */
class PresetEditor(
    private val presets: PresetRepository,
    private val newId: () -> PresetId = { PresetId(UUID.randomUUID().toString()) },
) {

    /** Makes a preset and returns it. */
    suspend fun create(
        name: String,
        cards: List<List<String>>,
        standardName: String,
        protectedKeys: Set<String> = emptySet(),
        fieldTypes: Map<String, String> = emptyMap(),
    ): EntryPreset {
        val existing = presets.presets.first()
        return save(newId(), name, cards, Marks(protectedKeys, fieldTypes), standardName, existing, editedId = null)
    }

    /**
     * Puts [name], [cards], [protectedKeys] and [fieldTypes] into the preset of [id] and returns it; its id - and
     * so its entries - stay.
     */
    suspend fun update(
        id: PresetId,
        name: String,
        cards: List<List<String>>,
        standardName: String,
        protectedKeys: Set<String> = emptySet(),
        fieldTypes: Map<String, String> = emptyMap(),
    ): EntryPreset {
        val existing = presets.presets.first()
        if (existing.none { it.id == id }) throw PresetFailure.NotFound()
        return save(id, name, cards, Marks(protectedKeys, fieldTypes), standardName, existing, editedId = id)
    }

    /**
     * Deletes the preset of [id]. Its entries keep their fields and their link: they open with the standard
     * preset, the fields of this one as their own (pass-docs, `vault-our.md`).
     */
    suspend fun delete(id: PresetId) {
        presets.delete(id)
    }

    private suspend fun save(
        id: PresetId,
        name: String,
        cards: List<List<String>>,
        marks: Marks,
        standardName: String,
        existing: List<EntryPreset>,
        editedId: PresetId?,
    ): EntryPreset {
        val normalizedName = PresetValidator.normalize(name)
        val named = cards.ifEmpty { listOf(emptyList()) }.map { keys -> keys.map(PresetValidator::normalize) }
        val problems = PresetValidator.problems(normalizedName, named, standardName, existing, editedId)
        if (problems.isNotEmpty()) throw PresetFailure.Invalid(problems)

        // The first card is always there - the user name and the password are in it
        val kept = listOf(named.first()) + named.drop(1).filter { it.isNotEmpty() }
        val preset = CustomPreset.of(
            id,
            normalizedName,
            kept,
            marks.protectedKeys.map(PresetValidator::normalize).toSet(),
            marks.fieldTypes.mapKeys { PresetValidator.normalize(it.key) },
        )
        presets.save(preset)
        return preset
    }

    /** What the person sets on the fields besides their names, by name. */
    private class Marks(val protectedKeys: Set<String>, val fieldTypes: Map<String, String>)
}
