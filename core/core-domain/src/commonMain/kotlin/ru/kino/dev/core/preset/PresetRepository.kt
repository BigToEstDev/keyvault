package ru.kino.dev.core.preset

import kotlinx.coroutines.flow.Flow

/**
 * The presets of one's own, kept by the app - one list for every database (pass-docs, Step 44, answer 4).
 * The standard preset is not among them: it is [StandardPreset], never stored.
 *
 * Changes go through [PresetEditor], which checks them; this is what it is built on. Throws [PresetFailure]
 * and nothing else.
 */
interface PresetRepository {

    /**
     * The presets in the order they were made. Empty when the store cannot be read: entries then open with
     * the standard preset, which is what a preset gone means anyway.
     */
    val presets: Flow<List<EntryPreset>>

    /** Puts [preset] in place of the one with its id, or after the others when there is none. */
    suspend fun save(preset: EntryPreset)

    /** Removes the preset of [id]; none such - nothing happens. */
    suspend fun delete(id: PresetId)
}

/**
 * Every way changing presets can fail, as a screen answers it. The message is for the log; the screen shows
 * its own text for each case.
 */
sealed class PresetFailure(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** What the preset cannot be saved with - every problem at once, so the screen marks them all. */
    class Invalid(val problems: List<PresetProblem>) : PresetFailure("The preset cannot be saved as it is")

    /** The preset is gone - deleted on another screen. */
    class NotFound : PresetFailure("The preset is not there")

    /** Writing the store failed; nothing changed. */
    class NotSaved(cause: Throwable? = null) : PresetFailure("The presets are not written", cause)
}
