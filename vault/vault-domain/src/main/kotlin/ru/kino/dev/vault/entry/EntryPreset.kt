package ru.kino.dev.vault.entry

/**
 * A field a preset shows on every entry of it.
 *
 * @param key the key in the file - for the kdbx standard ones the standard key, so other programs know it
 * @param required must be filled: when the entry is created, and must not be emptied after (pass-docs,
 *   Step 42, decision 11)
 * @param protected protected in the file when the entry is new; a field read from a file keeps its own
 * @param writtenWhenEmpty written into the file even when empty - the five kdbx standard fields, as KeePass
 *   writes them. A field of ours is written only when filled, so saving does not give every entry, a
 *   foreign one too, an empty field of its own
 */
data class PresetField(
    val key: String,
    val required: Boolean = false,
    val protected: Boolean = false,
    val writtenWhenEmpty: Boolean = false,
)

/**
 * A preset: the fields an entry is shown with first, in their order. Everything else the entry holds is
 * its own fields, after them.
 *
 * The core knows no preset (pass-docs, Step 45): an entry comes as its strings in the order of the file,
 * and the preset is laid over them here - [layout] on the way in, [EntryContent.fields] on the way out.
 */
data class EntryPreset(val fields: List<PresetField>) {

    init {
        require(fields.map { it.key }.toSet().size == fields.size) { "a key twice in a preset" }
    }

    /** Whether [name] is a key of this preset, ignoring case - the rule for the names of own fields. */
    fun hasKey(name: String): Boolean = fields.any { it.key.equals(name, ignoreCase = true) }

    /**
     * The fields of an entry laid over this preset. A field of the preset is found by its key, exactly as
     * the file spells it; one the entry lacks is empty. Everything else is an own field, in the order of
     * the file - a second field under a key of the preset too: an entry of another program may hold one,
     * and the person renames or deletes it.
     */
    fun layout(fields: List<EntryField>): EntryContent {
        val rest = fields.toMutableList()
        val presetFields = this.fields.map { presetField ->
            val index = rest.indexOfFirst { it.key == presetField.key }
            if (index >= 0) rest.removeAt(index) else presetField.empty()
        }
        return EntryContent(this, presetFields, rest)
    }

    /** The content of a new entry: every field of the preset empty, protected as the preset says. */
    fun blank(): EntryContent = EntryContent(this, fields.map { it.empty() }, emptyList())

    private fun PresetField.empty() = EntryField(key, value = "", protected = protected)
}

/**
 * The preset every entry is shown with until presets of one's own come (Step 44): the kdbx standard
 * fields and the name of the service. Decided by the owner, Step 43; the order is the order of the form.
 */
object StandardPreset {

    const val TITLE = "Title"
    const val USER_NAME = "UserName"
    const val PASSWORD = "Password"
    const val SERVICE = "Service"
    const val URL = "URL"
    const val NOTES = "Notes"

    val preset = EntryPreset(
        listOf(
            PresetField(TITLE, required = true, writtenWhenEmpty = true),
            PresetField(USER_NAME, required = true, writtenWhenEmpty = true),
            PresetField(PASSWORD, required = true, protected = true, writtenWhenEmpty = true),
            PresetField(SERVICE),
            PresetField(URL, writtenWhenEmpty = true),
            PresetField(NOTES, writtenWhenEmpty = true),
        ),
    )
}
