package ru.kino.dev.core.preset

import ru.kino.dev.core.preset.StandardPreset.NOTES
import ru.kino.dev.core.preset.StandardPreset.PASSWORD
import ru.kino.dev.core.preset.StandardPreset.USER_NAME

/**
 * How a preset of one's own is built (pass-docs, Step 44): the title; the first card - the user name and the
 * password, required, and after them the fields the person adds to it; the cards the person adds; the notes,
 * a card of their own, last.
 *
 * What the person sets is only the names: of the preset and of its fields. The rest is fixed here, so a
 * preset is stored as its names and built back by [of].
 */
object CustomPreset {

    /** The keys a field of one's own takes in no preset - the standard fields every one of them has. */
    val FIXED_KEYS: Set<String> = setOf(StandardPreset.TITLE, USER_NAME, PASSWORD, NOTES)

    /** The fields the first card opens with - the user name and the password - before the person's own. */
    const val FIRST_CARD_FIXED: Int = 2

    private val USER_NAME_FIELD = PresetField(USER_NAME, required = true, writtenWhenEmpty = true)
    private val PASSWORD_FIELD = PresetField(PASSWORD, required = true, protected = true, writtenWhenEmpty = true)
    private val NOTES_CARD = PresetCard(listOf(PresetField(NOTES, writtenWhenEmpty = true)))

    /**
     * The preset of [id] named [name] with the fields the person put in it.
     *
     * @param cards the names of the fields of each card, in order: the first is what goes after the user
     *   name and the password - empty when nothing does; the rest are the cards the person added. A field the
     *   person names is plain text, not required, written only when filled
     */
    fun of(id: PresetId, name: String, cards: List<List<String>>): EntryPreset {
        require(cards.isNotEmpty()) { "a preset has its first card" }
        val first = PresetCard(listOf(USER_NAME_FIELD, PASSWORD_FIELD) + cards.first().map(::PresetField))
        val added = cards.drop(1).map { keys -> PresetCard(keys.map(::PresetField)) }
        return EntryPreset(id, name, listOf(first) + added + NOTES_CARD)
    }

    /** What [of] was built from: the names of the fields the person put in each card. */
    fun cards(preset: EntryPreset): List<List<String>> {
        require(!preset.isStandard) { "the standard preset is not one's own" }
        val own = preset.cards.dropLast(1)
        return listOf(own.first().fields.drop(FIRST_CARD_FIXED).map { it.key }) + own.drop(1).map { card -> card.fields.map { it.key } }
    }
}

/** The limits on presets of one's own, decided by the owner (pass-docs, Step 44). */
object PresetLimits {

    /** The most presets a person has, the standard one included: no button to add one past it. */
    const val MAX_PRESETS: Int = 10

    /** The longest name of a preset, in characters. */
    const val MAX_NAME_LENGTH: Int = 64

    /**
     * The most fields a card takes - of a preset, or the own fields of an entry: no button to add one past it
     * (pass-docs, Step 40, fix 19.1).
     */
    const val MAX_CARD_FIELDS: Int = 12

    /**
     * The most cards of a preset of one's own, the first one - with the user name and the password - counted
     * and the notes not: no button to add one past it (pass-docs, Step 40, fix 19.2).
     */
    const val MAX_CARDS: Int = 6
}
