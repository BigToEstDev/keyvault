package ru.kino.dev.settings.pres.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import ru.kino.dev.settings.appearance.AppLanguage
import ru.kino.dev.settings.appearance.AppearanceRepository
import ru.kino.dev.settings.appearance.ThemeMode
import javax.inject.Inject

/**
 * The mode of the theme and the language for the whole app - the activity paints every screen with them,
 * `welcome` too (pass-docs, Step 55, Step 57).
 *
 * Not a screen: the activity keeps the system splash on until both have a value, so the app does not
 * flash in the other theme or language first.
 */
@HiltViewModel
class AppThemeHolder @Inject constructor(
    repository: AppearanceRepository,
) : ViewModel() {

    /** Null while the setting is being read; a new value as soon as it is changed in the settings. */
    val themeMode: StateFlow<ThemeMode?> = repository.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Null while the setting is being read; a new value as soon as it is changed in the settings. */
    val language: StateFlow<AppLanguage?> = repository.language
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
