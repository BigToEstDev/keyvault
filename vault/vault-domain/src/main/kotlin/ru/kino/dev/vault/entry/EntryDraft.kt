package ru.kino.dev.vault.entry

import ru.kino.dev.core.preset.EntryPreset

/**
 * The form of an entry being created or edited, as the person changes its preset back and forth before
 * saving (pass-docs, Step 44, answer 1): what the form shows, and what it hides.
 *
 * Kept in memory only, never in the saved state of a screen - values are secrets more often than not.
 *
 * @param hidden filled fields of a preset chosen before that the preset now chosen lacks: they come back
 *   when a preset with their key is chosen again. Saving writes [content] alone - these are lost, and the
 *   person is warned first (see [losesHidden])
 */
data class EntryDraft(
    val content: EntryContent,
    val hidden: List<EntryField> = emptyList(),
) {

    /** Saving now loses the values in [hidden] - the person is asked first. */
    val losesHidden: Boolean get() = hidden.isNotEmpty()

    /**
     * The draft with [preset] chosen. Each field of the new preset takes its value by key, from the first
     * that has it: a field of the preset now shown, an own field - it moves into the preset, a hidden one.
     * A field of the preset now shown that the new one lacks is hidden when filled, dropped when empty. Own
     * fields stay own.
     */
    fun withPreset(preset: EntryPreset): EntryDraft {
        val own = content.ownFields.toMutableList()
        val presetFields = preset.fields.map { presetField ->
            val key = presetField.key
            content.presetFields.firstOrNull { it.key == key }
                ?: own.indexOfFirst { it.key == key }.takeIf { it >= 0 }?.let { own.removeAt(it) }
                ?: hidden.firstOrNull { it.key == key }
                ?: presetField.empty()
        }
        val left = content.presetFields.filter { !preset.hasExactKey(it.key) && it.value.isNotEmpty() }
        val stillHidden = hidden.filter { field -> !preset.hasExactKey(field.key) && left.none { it.key == field.key } }
        return EntryDraft(EntryContent(preset, presetFields, own), stillHidden + left)
    }

    private fun EntryPreset.hasExactKey(key: String) = fields.any { it.key == key }
}
