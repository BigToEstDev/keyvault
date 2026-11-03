package ru.kino.dev.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ru.kino.dev.core.phone.PhoneNumbers
import ru.kino.dev.navigation.AppNavHost
import ru.kino.dev.navigation.AppNavigator
import ru.kino.dev.navigation.LocalNavigator
import ru.kino.dev.settings.appearance.AppLanguage
import ru.kino.dev.settings.appearance.ThemeMode
import ru.kino.dev.settings.pres.theme.AppThemeHolder
import ru.kino.dev.ui.phone.LocalPhoneNumbers
import ru.kino.dev.ui.theme.AppLocale
import ru.kino.dev.ui.theme.KeyVaultTheme
import ru.kino.dev.ui.theme.LocalAppDisplay
import ru.kino.dev.ui.theme.ProvideLayoutType
import ru.kino.dev.vault.lock.AutoLock
import ru.kino.dev.welcome.pres.start.WelcomeStartResolver
import java.util.Locale

/**
 * The app: its theme, its language and what the screens show - as chosen in the settings - over the navigation of all
 * features. The same on android and on the desktop; what the platform does around it stays with its app
 * (pass-docs, Step 88).
 *
 * Nothing is drawn until the start route and the appearance are read - android keeps its splash up meanwhile, so the
 * app does not flash in the other theme or language first (pass-docs, Step 55, 57, 58).
 *
 * @param onDarkTheme the theme the app is drawn in, for what the platform draws around it: the bars of android
 */
@Composable
fun App(onDarkTheme: (Boolean) -> Unit = {}) {
    val theme = koinViewModel<AppThemeHolder>()
    val startResolver = koinViewModel<WelcomeStartResolver>()

    val mode by theme.themeMode.collectAsStateWithLifecycle()
    val darkTheme = when (mode ?: return) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    LaunchedEffect(darkTheme) { onDarkTheme(darkTheme) }

    val language by theme.language.collectAsStateWithLifecycle()
    val display by theme.display.collectAsStateWithLifecycle()
    val shown = display ?: return
    AppLocale(locale = (language ?: return).locale) {
        val navigator = koinInject<AppNavigator>()
        // What the screens show, as chosen in the settings: the features read it, not `settings`. The navigator of the
        // screens - the app's; a container gives its own below (pass-docs, Step 98)
        CompositionLocalProvider(
            LocalAppDisplay provides shown,
            LocalPhoneNumbers provides koinInject<PhoneNumbers>(),
            LocalNavigator provides navigator,
        ) {
            KeyVaultTheme(darkTheme = darkTheme) {
                val start by startResolver.start.collectAsStateWithLifecycle()
                // The layout by the width of the window, for the screens with layouts of their own (pass-docs, Step 94)
                ProvideLayoutType {
                    start?.let {
                        AppNavHost(navigator = navigator, autoLock = koinInject<AutoLock>(), start = it)
                    }
                }
            }
        }
    }
}

/** The language of the app to set; null - the language of the system. */
val AppLanguage.locale: Locale?
    get() = when (this) {
        AppLanguage.System -> null
        AppLanguage.English -> Locale.ENGLISH
        AppLanguage.Russian -> Locale.forLanguageTag("ru")
    }
