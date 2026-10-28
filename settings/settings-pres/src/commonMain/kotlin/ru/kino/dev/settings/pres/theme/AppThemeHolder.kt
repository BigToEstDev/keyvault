package ru.kino.dev.settings.pres.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import ru.kino.dev.settings.appearance.AppLanguage
import ru.kino.dev.settings.appearance.AppearanceRepository
import ru.kino.dev.settings.appearance.ThemeMode
import ru.kino.dev.settings.lock.LockSettingsRepository
import ru.kino.dev.settings.presets.PresetSettingsRepository
import ru.kino.dev.ui.theme.AppDisplay

/**
 * The mode of the theme, the language and what the screens show for the whole app - the activity paints every
 * screen with them, `welcome` too (pass-docs, Step 55, Step 57, Step 58).
 *
 * Not a screen: the activity keeps the system splash on until all have a value, so the app does not
 * flash in the other theme or language first.
 */
class AppThemeHolder(
    repository: AppearanceRepository,
    lock: LockSettingsRepository,
    presets: PresetSettingsRepository,
) : ViewModel() {

    /** Null while the setting is being read; a new value as soon as it is changed in the settings. */
    val themeMode: StateFlow<ThemeMode?> = repository.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Null while the setting is being read; a new value as soon as it is changed in the settings. */
    val language: StateFlow<AppLanguage?> = repository.language
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * What the screens show - all the switches of it at once, so a new one does not reach the activity. Null
     * while they are being read; a new value as soon as one is changed in the settings.
     */
    val display: StateFlow<AppDisplay?> = combine(
        // A typed combine takes five at most: what the list of entries shows goes as one
        combine(
            repository.userNameInList,
            repository.entryCount,
            repository.entryCountNested,
        ) { userNameInList, entryCount, entryCountNested ->
            AppDisplay(userNameInList = userNameInList, entryCount = entryCount, entryCountNested = entryCountNested)
        },
        // ...and what a password shows - its colors and its strength
        combine(repository.passwordColoring, repository.passwordStrength, ::Pair),
        lock.lockButton,
        presets.usePresets,
    ) { list, (passwordColoring, passwordStrength), lockButton, usePresets ->
        list.copy(
            passwordColoring = passwordColoring,
            passwordStrength = passwordStrength,
            lockButton = lockButton,
            presets = usePresets,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
