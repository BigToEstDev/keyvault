package ru.kino.dev.settings.privacy

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.IOException

/** The privacy settings in memory; [failSave] - nothing is written, as with a full disk. */
class FakePrivacySettingsRepository(
    allowScreenshots: Boolean = false,
    private val failSave: Boolean = false,
) : PrivacySettingsRepository {

    private val allowing = MutableStateFlow(allowScreenshots)

    val savedAllowScreenshots = mutableListOf<Boolean>()

    override val allowScreenshots: Flow<Boolean> = allowing

    override suspend fun saveAllowScreenshots(on: Boolean) {
        if (failSave) throw IOException("disk full")
        savedAllowScreenshots += on
        allowing.value = on
    }
}
