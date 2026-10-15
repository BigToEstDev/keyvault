package ru.kino.dev.vault.entry

import ru.kino.dev.core.preset.PresetLink

/**
 * The rules of the fields of an entry, for both sides: the screen marks the fields, [EntryEditor] refuses
 * the save.
 *
 * Own and foreign entries are not told apart - the rules look at what the entry held when it was read
 * (pass-docs, Step 42, decision 11). What another program wrote stays as it is; what a person writes here
 * follows the rules.
 */
object EntryValidator {

    /** The name of an own field as it is stored: without the spaces around it. */
    fun normalizeName(name: String): String = name.trim()

    /**
     * The problem with [name] as the name of an own field of [content], or null when there is none - for
     * adding a field and renaming one.
     *
     * A name is not empty, not a key of the preset, not the key of the link to a preset ([PresetLink]) and
     * not the name of another own field, ignoring case and the spaces around it. There is no limit on its length: a limit would lock entries of other programs
     * (pass-docs, Step 42, decision 13).
     *
     * @param renamedIndex the own field being renamed - its current name is not a rival
     */
    fun nameProblem(content: EntryContent, name: String, renamedIndex: Int? = null): NameProblem? {
        val normalized = normalizeName(name)
        return when {
            normalized.isEmpty() -> NameProblem.EMPTY
            content.preset.hasKey(normalized) -> NameProblem.TAKEN
            normalized.equals(PresetLink.KEY, ignoreCase = true) -> NameProblem.TAKEN
            content.ownFields.withIndex().any { (index, field) ->
                index != renamedIndex && normalizeName(field.key).equals(normalized, ignoreCase = true)
            } -> NameProblem.TAKEN
            else -> null
        }
    }

    /**
     * Everything that stops [edited] from being saved, or an empty list.
     *
     * - A required field of the preset is filled when the entry is new, and is not emptied when it is
     *   [read] - one that was empty in the file already is not asked for.
     * - The name of an own field that was not there when the entry was read - added or renamed here - follows
     *   [nameProblem].
     * - The name of an own field as it was read is only checked for what the core refuses: a key the entry
     *   holds already, exactly as spelled. Another program may have written two fields under one key; the
     *   person renames or deletes one of them.
     *
     * @param read the entry as it was read, null for a new one
     */
    fun problems(edited: EntryContent, read: EntryContent?): List<FieldProblem> {
        val required = edited.preset.fields.filter { it.required }.mapNotNull { field ->
            val emptied = edited.value(field.key).isEmpty()
            val wasFilled = read == null || read.value(field.key).isNotEmpty()
            if (emptied && wasFilled) FieldProblem.Required(field.key) else null
        }

        val readNames = read?.ownFields?.map { it.key }?.toSet().orEmpty()
        val presetKeys = edited.preset.fields.map { it.key }.toSet()
        val names = edited.ownFields.withIndex().mapNotNull { (index, field) ->
            val problem = if (field.key in readNames) {
                val twin = field.key in presetKeys || edited.ownFields.take(index).any { it.key == field.key }
                if (twin) NameProblem.TAKEN else null
            } else {
                nameProblem(edited, field.key, renamedIndex = index)
            }
            problem?.let { FieldProblem.BadName(index, it) }
        }

        return required + names
    }
}
