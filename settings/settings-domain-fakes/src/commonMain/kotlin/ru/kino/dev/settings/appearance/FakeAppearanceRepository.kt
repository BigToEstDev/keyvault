package ru.kino.dev.settings.appearance

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.IOException

/**
 * How the app looks, in memory: what was saved, in order. [failSave] - every save throws, as a disk that is full.
 */
class FakeAppearanceRepository(
    initial: ThemeMode = ThemeMode.System,
    initialLanguage: AppLanguage = AppLanguage.System,
    private val failSave: Boolean = false,
    initialUserNameInList: Boolean = false,
) : AppearanceRepository {

    private val mode = MutableStateFlow(initial)

    private val chosenLanguage = MutableStateFlow(initialLanguage)

    val saved = mutableListOf<ThemeMode>()

    val savedLanguages = mutableListOf<AppLanguage>()

    val savedColoring = mutableListOf<Boolean>()

    private val coloring = MutableStateFlow(true)

    val savedStrength = mutableListOf<Boolean>()

    private val strength = MutableStateFlow(false)

    val savedUserNameInList = mutableListOf<Boolean>()

    private val userNames = MutableStateFlow(initialUserNameInList)

    val savedEntryCount = mutableListOf<Boolean>()

    private val counts = MutableStateFlow(true)

    val savedEntryCountNested = mutableListOf<Boolean>()

    private val nested = MutableStateFlow(true)

    override val themeMode: Flow<ThemeMode> = mode

    override val language: Flow<AppLanguage> = chosenLanguage

    override val passwordColoring: Flow<Boolean> = coloring

    override val passwordStrength: Flow<Boolean> = strength

    override val userNameInList: Flow<Boolean> = userNames

    override val entryCount: Flow<Boolean> = counts

    override val entryCountNested: Flow<Boolean> = nested

    override suspend fun save(mode: ThemeMode) {
        if (failSave) throw IOException("disk full")
        saved += mode
        this.mode.value = mode
    }

    override suspend fun save(language: AppLanguage) {
        if (failSave) throw IOException("disk full")
        savedLanguages += language
        chosenLanguage.value = language
    }

    override suspend fun savePasswordColoring(on: Boolean) {
        if (failSave) throw IOException("disk full")
        savedColoring += on
        coloring.value = on
    }

    override suspend fun savePasswordStrength(on: Boolean) {
        if (failSave) throw IOException("disk full")
        savedStrength += on
        strength.value = on
    }

    override suspend fun saveUserNameInList(on: Boolean) {
        if (failSave) throw IOException("disk full")
        savedUserNameInList += on
        userNames.value = on
    }

    override suspend fun saveEntryCount(on: Boolean) {
        if (failSave) throw IOException("disk full")
        savedEntryCount += on
        counts.value = on
    }

    override suspend fun saveEntryCountNested(on: Boolean) {
        if (failSave) throw IOException("disk full")
        savedEntryCountNested += on
        nested.value = on
    }
}
