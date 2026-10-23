package ru.kino.dev.ui.theme

import android.content.res.Resources
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.window.Dialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.kino.dev.core.pres.R
import java.util.Locale

/**
 * A dialog and a menu are windows of their own, on the context of the activity: with the system in Russian and
 * the app in English, what they show is in English too (pass-docs, Step 72, fix 1).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ru")
class LocaleWindowsTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun `a dialog is in the language of the app`() {
        show { Dialog(onDismissRequest = {}) { Text(stringResource(R.string.action_close)) } }

        compose.onNodeWithText("Close").assertExists()
        compose.onNodeWithText("Закрыть").assertDoesNotExist()
    }

    @Test
    fun `a menu is in the language of the app`() {
        show { DropdownMenu(expanded = true, onDismissRequest = {}) { Text(stringResource(R.string.action_close)) } }

        compose.onNodeWithText("Close").assertExists()
        compose.onNodeWithText("Закрыть").assertDoesNotExist()
    }

    // As the app does it: the activity in the language of the app, the screen under `AppLocale`
    private fun show(content: @Composable () -> Unit) {
        val activity = Robolectric.buildActivity(LocalizedActivity::class.java).setup().get()
        activity.localeResources.locale = Locale.ENGLISH
        activity.setContent { AppLocale(locale = Locale.ENGLISH, content = content) }
        compose.waitForIdle()
    }

    // `MainActivity` in short: its resources go through [LocaleResources]
    class LocalizedActivity : ComponentActivity() {
        val localeResources = LocaleResources()

        override fun getResources(): Resources = localeResources.of(baseContext, super.getResources())
    }
}
