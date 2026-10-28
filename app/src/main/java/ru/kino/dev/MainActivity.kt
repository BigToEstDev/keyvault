package ru.kino.dev

import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.kino.dev.app.App
import ru.kino.dev.app.locale
import ru.kino.dev.settings.lock.InactivityTimer
import ru.kino.dev.settings.pres.theme.AppThemeHolder
import ru.kino.dev.settings.privacy.PrivacySettingsRepository
import ru.kino.dev.ui.theme.LocaleResources
import ru.kino.dev.welcome.pres.start.WelcomeStartResolver

/**
 * A [FragmentActivity] rather than a plain component one: `BiometricPrompt` shows itself through the
 * fragment manager.
 */
class MainActivity : FragmentActivity() {

    // Told what the app sees; `vault` listens to it (pass-docs, Step 59)
    private val timer: InactivityTimer by inject()

    private val privacy: PrivacySettingsRepository by inject()

    // The same as the root of `App` takes - the store of the activity: the splash waits for them
    private val startResolver: WelcomeStartResolver by viewModel()

    private val theme: AppThemeHolder by viewModel()

    private val localeResources = LocaleResources()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before super: the splash replaces the launch theme. It stays up until the start route is known -
        // reading the remembered database takes a moment, and there is nothing to show before it - and the
        // appearance, so the app does not flash in the other theme or language first (pass-docs, Step 55, 57, 58)
        installSplashScreen().setKeepOnScreenCondition {
            startResolver.start.value == null || theme.themeMode.value == null || theme.language.value == null ||
                theme.display.value == null
        }
        super.onCreate(savedInstanceState)

        // The whole app, not a screen: there are more screens with secrets than without. Closed before the
        // setting is read, then as chosen in the settings - lifted and put back without a restart; debug as
        // release (pass-docs, Step 61)
        secureWindow(true)
        lifecycleScope.launch { privacy.allowScreenshots.collect { secureWindow(!it) } }
        // Out of autofill, the whole window: no other manager reads or offers to keep our passwords. The fields
        // take the windows of dialogs out themselves (pass-docs, Step 74, item 5)
        window.decorView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        // The windows of dialogs and menus take the language from the activity, not from `AppLocale`
        // (pass-docs, Step 72, fix 1)
        lifecycleScope.launch { theme.language.collect { localeResources.locale = it?.locale } }

        enableEdgeToEdge()
        // The app itself is common to the platforms; the bars of the system follow its theme, not the system's
        setContent { App(onDarkTheme = ::systemBars) }
    }

    // In the language of the app - for the windows built on the activity: dialogs, menus, popups. The screens
    // take it from `AppLocale`, which also redraws them when the language changes (pass-docs, Step 72, fix 1)
    override fun getResources(): Resources = localeResources.of(baseContext, super.getResources())

    // Screenshots, recording, casting and the preview in the recent apps - all at once, Android does not tell
    // them apart
    private fun secureWindow(secure: Boolean) {
        if (secure) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // The scrims are the defaults of `enableEdgeToEdge`: only how dark the theme is decided here
    private fun systemBars(darkTheme: Boolean) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
            navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { darkTheme },
        )
    }

    // Before the first frame after coming back: a timer slept through in the background locks now, and
    // `vault` is covered before it could be seen (pass-docs, Step 46)
    override fun onStart() {
        super.onStart()
        timer.cameBack()
    }

    // Put away or the screen off - leaving, which may lock at once (pass-docs, Step 59). Not a turn of the
    // screen: the activity is built anew then, and the person is still here
    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) timer.wentAway()
    }

    // Every touch and key of the app, whatever screen takes it - the person is here. The callback of the
    // Activity for exactly this: a touch counts once, as it starts, not on every move (pass-docs, Step 40, fix 14)
    override fun onUserInteraction() {
        super.onUserInteraction()
        timer.touched()
    }

    private companion object {
        // The scrims `enableEdgeToEdge` puts under three-button navigation by default
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
