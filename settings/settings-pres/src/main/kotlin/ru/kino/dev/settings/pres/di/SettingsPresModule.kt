package ru.kino.dev.settings.pres.di

import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ru.kino.dev.settings.pres.appearance.AppearanceProcessor
import ru.kino.dev.settings.pres.entries.EntriesProcessor
import ru.kino.dev.settings.pres.file.FileProcessor
import ru.kino.dev.settings.pres.lock.LockProcessor
import ru.kino.dev.settings.pres.presetedit.PresetEditProcessor
import ru.kino.dev.settings.pres.presets.PresetsProcessor
import ru.kino.dev.settings.pres.privacy.PrivacyProcessor
import ru.kino.dev.settings.pres.recyclebin.RecycleBinProcessor
import ru.kino.dev.settings.pres.security.SecurityProcessor
import ru.kino.dev.settings.pres.settings.SettingsProcessor
import ru.kino.dev.settings.pres.theme.AppThemeHolder

/**
 * The screens of `settings`. What a screen gives its processor (`parametersOf` on the screen) comes first in the
 * lambda, the rest is the graph's.
 */
val settingsPresModule = module {
    // Read by the activity: the theme, the language and the display of the whole app
    viewModelOf(::AppThemeHolder)

    viewModelOf(::PresetsProcessor)
    viewModelOf(::PrivacyProcessor)

    viewModel { (dbKey: String) -> SettingsProcessor(dbKey, get()) }
    viewModel { (dbKey: String) -> AppearanceProcessor(dbKey, get(), get(), get(), get()) }
    viewModel { (dbKey: String) -> EntriesProcessor(dbKey, get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { (dbKey: String) -> FileProcessor(dbKey, get(), get(), get(), get()) }
    viewModel { (dbKey: String) -> LockProcessor(dbKey, get(), get(), get(), get(), get(), get(), get()) }
    viewModel { (dbKey: String) -> RecycleBinProcessor(dbKey, get(), get(), get(), get()) }
    viewModel { (dbKey: String) -> SecurityProcessor(dbKey, get(), get(), get(), get(), get()) }
    viewModel { (presetId: String?) -> PresetEditProcessor(presetId, get(), get(), get(), get()) }
}
