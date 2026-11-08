package ru.kino.dev.settings.pres.merge

/** What the page `Синхронизация` asks of its screen once (pass-docs, Step 111). */
sealed interface MergeSideEffect {

    /** "Save a copy": the file to save it into, the [name] of this database's file suggested. */
    data class PickCopyTarget(val name: String) : MergeSideEffect

    /** "Send": the share menu of the system for the file at [uri], put where another app reads it. */
    data class Share(val uri: String) : MergeSideEffect
}
