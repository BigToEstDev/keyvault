package ru.kino.dev

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import ru.kino.dev.navigation.AppNavHost
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.ui.theme.PassappTheme
import ru.kino.dev.vault.lock.AutoLock
import ru.kino.dev.welcome.pres.start.WelcomeStartResolver
import javax.inject.Inject

/**
 * A [FragmentActivity] rather than a plain component one: `BiometricPrompt` shows itself through the
 * fragment manager.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var autoLock: AutoLock

    private val startResolver: WelcomeStartResolver by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before super: the splash replaces the launch theme. It stays up until the start route is known -
        // reading the remembered database takes a moment, and there is nothing to show before it
        installSplashScreen().setKeepOnScreenCondition { startResolver.start.value == null }
        super.onCreate(savedInstanceState)

        // The whole app, not a screen: there are more screens with secrets than without, and a switch in
        // settings would be an offer to make oneself less safe (pass-docs, welcome decisions.md).
        // Not in debug: screenshots are needed to review the UI
        if (!BuildConfig.DEBUG) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }

        enableEdgeToEdge()
        setContent {
            PassappTheme {
                val start by startResolver.start.collectAsStateWithLifecycle()
                start?.let { AppNavHost(navigator = navigator, autoLock = autoLock, start = it) }
            }
        }
    }

    // Before the first frame after coming back: a timer slept through in the background locks now, and
    // `vault` is covered before it could be seen (pass-docs, Step 46)
    override fun onStart() {
        super.onStart()
        autoLock.cameBack()
    }

    // Every touch and key of the app, whatever screen takes it - the person is here
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        autoLock.touched()
        return super.dispatchTouchEvent(event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        autoLock.touched()
        return super.dispatchKeyEvent(event)
    }
}
