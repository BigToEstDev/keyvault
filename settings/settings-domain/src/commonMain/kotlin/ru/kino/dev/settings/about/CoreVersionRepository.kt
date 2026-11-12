package ru.kino.dev.settings.about

/** The version of the core the app is built with - asked of the core itself (pass-docs, Step 123). */
interface CoreVersionRepository {

    /** SemVer of the core: `1.0.0`. */
    suspend fun coreVersion(): String
}
