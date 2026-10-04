package ru.kino.dev

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import ru.kino.dev.navigation.AppNavHost
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.ui.theme.PassappTheme
import ru.kino.dev.welcome.pres.start.WelcomeStartResolver
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var navigator: Navigator

    private val startResolver: WelcomeStartResolver by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before super: the splash replaces the launch theme. It stays up until the start route is known -
        // reading the remembered database takes a moment, and there is nothing to show before it
        installSplashScreen().setKeepOnScreenCondition { startResolver.start.value == null }
        super.onCreate(savedInstanceState)

        // The whole app, not a screen: there are more screens with secrets than without, and a switch in
        // settings would be an offer to make oneself less safe (pass-docs, welcome decisions.md)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        enableEdgeToEdge()
        setContent {
            PassappTheme {
                val start by startResolver.start.collectAsStateWithLifecycle()
                start?.let { AppNavHost(navigator = navigator, start = it) }
            }
        }
    }
}
