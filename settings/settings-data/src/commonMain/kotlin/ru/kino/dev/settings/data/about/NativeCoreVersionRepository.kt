package ru.kino.dev.settings.data.about

import ru.kino.dev.core.NativeCore
import ru.kino.dev.settings.about.CoreVersionRepository

/** The version of the core - from the core through its bridge (pass-docs, Step 123). */
internal class NativeCoreVersionRepository(private val core: NativeCore) : CoreVersionRepository {

    override suspend fun coreVersion(): String = core.coreVersion()
}
