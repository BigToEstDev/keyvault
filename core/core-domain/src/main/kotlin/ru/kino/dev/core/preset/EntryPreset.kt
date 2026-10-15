package ru.kino.dev.core.preset

/** The id of a preset of one's own - a uuid, made once when the preset is created. */
@JvmInline
value class PresetId(val value: String)

/**
 * A field a preset shows on every entry of it.
 *
 * @param key the key in the file - for the kdbx standard ones the standard key, so other programs know it
 * @param required must be filled: when the entry is created, and must not be emptied after (pass-docs,
 *   Step 42, decision 11)
 * @param protected protected in the file when the entry is new; a field read from a file keeps its own
 * @param writtenWhenEmpty written into the file even when empty - the kdbx standard fields, as KeePass
 *   writes them. A field of ours is written only when filled, so saving does not give every entry, a
 *   foreign one too, an empty field of its own
 */
data class PresetField(
    val key: String,
    val required: Boolean = false,
    val protected: Boolean = false,
    val writtenWhenEmpty: Boolean = false,
)

/** One card of a preset: the fields the screens of an entry show together, in their order. */
data class PresetCard(val fields: List<PresetField>)

/**
 * A preset: the fields an entry is shown with first, card by card. Everything else the entry holds is its
 * own fields, after them.
 *
 * The title is no card's: every preset has it, first, in the card of the icon (pass-docs, `vault-our.md`).
 * [fields] is the title and then the fields of the cards, in order.
 *
 * The core knows no preset (pass-docs, Step 45): an entry comes as its strings in the order of the file, and
 * the preset is laid over them in `vault`.
 *
 * @param id null for the standard preset, which is not stored anywhere
 * @param name null for the standard preset: the screens name it from their resources
 */
data class EntryPreset(
    val id: PresetId?,
    val name: String?,
    val cards: List<PresetCard>,
) {

    /** The title, then the fields of the cards in order. */
    val fields: List<PresetField> = listOf(TITLE_FIELD) + cards.flatMap { it.fields }

    init {
        require(fields.map { it.key }.toSet().size == fields.size) { "a key twice in a preset" }
    }

    val isStandard: Boolean get() = id == null

    /** Whether [name] is a key of this preset, ignoring case - the rule for the names of own fields. */
    fun hasKey(name: String): Boolean = fields.any { it.key.equals(name, ignoreCase = true) }
}

// Top level, not in StandardPreset: built before any preset is, the standard one included
private val TITLE_FIELD = PresetField(StandardPreset.TITLE, required = true, writtenWhenEmpty = true)
