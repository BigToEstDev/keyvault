package ru.kino.dev.settings.pres.di

import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.navigation.PresetEditRoute
import ru.kino.dev.settings.pres.appearance.AppearanceProcessor
import ru.kino.dev.settings.pres.entries.EntriesProcessor
import ru.kino.dev.settings.pres.file.FileProcessor
import ru.kino.dev.settings.pres.lock.LockProcessor
import ru.kino.dev.settings.pres.presetedit.PresetEditProcessor
import ru.kino.dev.settings.pres.presetread.PresetReadProcessor
import ru.kino.dev.settings.pres.presets.PresetsProcessor
import ru.kino.dev.settings.pres.presetscontainer.PresetsContainerProcessor
import ru.kino.dev.settings.pres.privacy.PrivacyProcessor
import ru.kino.dev.settings.pres.recyclebin.RecycleBinProcessor
import ru.kino.dev.settings.pres.security.SecurityProcessor
import ru.kino.dev.settings.pres.settings.SettingsProcessor
import ru.kino.dev.settings.pres.theme.AppThemeHolder

/**
 * The screens of `settings`. What a screen gives its processor (`parametersOf` on the screen) comes first in the
 * lambda, the rest is the graph's. The navigator is the screen's too - `LocalNavigator`, not the graph's: inside a
 * container it is the container's (pass-docs, Step 98).
 */
val settingsPresModule = module {
    // Read by the activity: the theme, the language and the display of the whole app
    viewModelOf(::AppThemeHolder)

    viewModel { (navigator: Navigator) -> PresetsProcessor(get(), get(), get(), navigator, get()) }
    viewModel { (navigator: Navigator) -> PrivacyProcessor(get(), navigator) }

    viewModel { (dbKey: String, navigator: Navigator) -> SettingsProcessor(dbKey, navigator) }
    viewModel { (dbKey: String, navigator: Navigator) -> AppearanceProcessor(dbKey, get(), get(), navigator, get()) }
    viewModel { (dbKey: String, navigator: Navigator) ->
        EntriesProcessor(dbKey, get(), get(), get(), get(), get(), get(), navigator, get())
    }
    viewModel { (dbKey: String, navigator: Navigator) -> FileProcessor(dbKey, get(), get(), navigator, get()) }
    viewModel { (dbKey: String, navigator: Navigator) ->
        LockProcessor(dbKey, get(), get(), get(), get(), get(), navigator, get())
    }
    viewModel { (dbKey: String, navigator: Navigator) -> RecycleBinProcessor(dbKey, get(), get(), navigator, get()) }
    viewModel { (dbKey: String, navigator: Navigator) ->
        SecurityProcessor(dbKey, get(), get(), get(), navigator, get())
    }
    viewModel { (presetId: String?, navigator: Navigator) ->
        PresetEditProcessor(presetId, get(), get(), navigator, get())
    }
    viewModel { (presetId: String?, navigator: Navigator) ->
        PresetReadProcessor(presetId, get(), get(), navigator, get())
    }
    // The container of a wide window: the screens in it bring their own processors (pass-docs, Step 100, item 3)
    viewModel { (opening: PresetEditRoute?) -> PresetsContainerProcessor(opening, get()) }
}
