package ru.kino.dev.core.preset

/**
 * The rules of a preset of one's own, for both sides: the screen of a preset marks its fields, the editor
 * refuses the save (pass-docs, Step 44, answers 7 and 8).
 *
 * A preset is given as the person sets it: its name and the names of the fields of each card, as
 * [CustomPreset.of] takes them.
 */
object PresetValidator {

    /** A name - of a preset or of its field - as it is stored: without the spaces around it. */
    fun normalize(name: String): String = name.trim()

    /** Whether one more preset can be made next to [presets] - the ones of one's own. */
    fun canAdd(presets: List<EntryPreset>): Boolean = presets.size + 1 < PresetLimits.MAX_PRESETS

    /** Whether one more card can be added to [cards], as [CustomPreset.of] takes them. */
    fun canAddCard(cards: List<List<String>>): Boolean = cards.size < PresetLimits.MAX_CARDS

    /**
     * Whether one more field can go into the card [card] of [cards], as [CustomPreset.of] takes them: a card
     * holds up to [PresetLimits.MAX_CARD_FIELDS], the user name and the password of the first one counted.
     */
    fun canAddField(cards: List<List<String>>, card: Int): Boolean {
        val keys = cards.getOrNull(card) ?: return false
        val fixed = if (card == 0) CustomPreset.FIRST_CARD_FIXED else 0
        return fixed + keys.size < PresetLimits.MAX_CARD_FIELDS
    }

    /**
     * The problem with [name] as the name of a preset, or null when there is none. A name is not empty, not
     * longer than [PresetLimits.MAX_NAME_LENGTH] characters (code points, so an emoji is one) and not the
     * name of another preset, the standard one included, ignoring case.
     *
     * @param standardName what the screens call the standard preset - from their resources, in the language
     *   of the phone
     * @param presets the presets of one's own
     * @param editedId the preset being changed - its current name is not a rival
     */
    fun nameProblem(
        name: String,
        standardName: String,
        presets: List<EntryPreset>,
        editedId: PresetId? = null,
    ): PresetNameProblem? {
        val normalized = normalize(name)
        val rivals = presets.filter { it.id != editedId }.mapNotNull { it.name } + standardName
        return when {
            normalized.isEmpty() -> PresetNameProblem.EMPTY
            normalized.codePointCount(0, normalized.length) > PresetLimits.MAX_NAME_LENGTH -> PresetNameProblem.TOO_LONG
            rivals.any { normalize(it).equals(normalized, ignoreCase = true) } -> PresetNameProblem.TAKEN
            else -> null
        }
    }

    /**
     * The problem with [name] as the name of a field of a preset made of [cards], or null when there is
     * none - for adding a field and renaming one. A name is not empty, not a key every preset has or the key
     * of the link to the preset, and not the name of another field of the preset, ignoring case. There is no
     * limit on its length - as with own fields of an entry.
     *
     * @param renamed the field being renamed - its current name is not a rival
     */
    fun fieldNameProblem(cards: List<List<String>>, name: String, renamed: FieldPosition? = null): FieldNameProblem? {
        val normalized = normalize(name)
        val others = cards.flatMapIndexed { card, keys ->
            keys.filterIndexed { index, _ -> FieldPosition(card, index) != renamed }
        }
        return when {
            normalized.isEmpty() -> FieldNameProblem.EMPTY
            RESERVED_KEYS.any { it.equals(normalized, ignoreCase = true) } -> FieldNameProblem.TAKEN
            others.any { normalize(it).equals(normalized, ignoreCase = true) } -> FieldNameProblem.TAKEN
            else -> null
        }
    }

    /**
     * Everything that stops a preset from being saved, or an empty list: its name, the name of each field,
     * and - for a new one - the number of presets.
     *
     * @param editedId the preset being changed, null for a new one
     */
    fun problems(
        name: String,
        cards: List<List<String>>,
        standardName: String,
        presets: List<EntryPreset>,
        editedId: PresetId? = null,
    ): List<PresetProblem> {
        val tooMany = if (editedId == null && !canAdd(presets)) listOf(PresetProblem.TooMany) else emptyList()
        val badName = nameProblem(name, standardName, presets, editedId)?.let { listOf(PresetProblem.Name(it)) }
        val fields = cards.flatMapIndexed { card, keys ->
            keys.indices.mapNotNull { index ->
                val at = FieldPosition(card, index)
                fieldNameProblem(cards, keys[index], renamed = at)?.let { PresetProblem.FieldName(at, it) }
            }
        }
        return tooMany + badName.orEmpty() + fields
    }

    private val RESERVED_KEYS = CustomPreset.FIXED_KEYS + PresetLink.KEY
}

/** Where a field of a preset is: the card, counted as [CustomPreset.of] takes them, and its place in it. */
data class FieldPosition(val card: Int, val index: Int)

/** Why a preset cannot be saved. */
sealed class PresetProblem {

    /** There are [PresetLimits.MAX_PRESETS] already, the standard one included. */
    data object TooMany : PresetProblem()

    /** The name of the preset cannot be used. Shown under it. */
    data class Name(val problem: PresetNameProblem) : PresetProblem()

    /** The name of the field [at] cannot be used. Shown under it. */
    data class FieldName(val at: FieldPosition, val problem: FieldNameProblem) : PresetProblem()
}

/** What is wrong with the name of a preset. */
enum class PresetNameProblem {
    /** Nothing but spaces */
    EMPTY,

    /** Longer than [PresetLimits.MAX_NAME_LENGTH] */
    TOO_LONG,

    /** Another preset, the standard one included, is already called that */
    TAKEN,
}

/** What is wrong with the name of a field of a preset. */
enum class FieldNameProblem {
    /** Nothing but spaces */
    EMPTY,

    /** Another field of the preset is already called that, or every preset has a field so named */
    TAKEN,
}
