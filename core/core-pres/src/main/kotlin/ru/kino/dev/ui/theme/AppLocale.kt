package ru.kino.dev.ui.theme

import android.content.res.Configuration
import android.os.LocaleList
import android.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * [content] in [locale], without recreating the activity - the screen, the open database and the back stack
 * stay as they are (pass-docs, Step 57). Null - the language of the system, nothing is replaced.
 *
 * `stringResource` and the strings of Material 3 read the resources of [LocalContext] and follow
 * [LocalConfiguration], so both are replaced. The context wraps the activity rather than being a new
 * configuration context: `BiometricPrompt` and the like look for the activity through it. Strings taken
 * outside the composition - from the application context - stay in the language of the system.
 */
@Composable
fun AppLocale(locale: Locale?, content: @Composable () -> Unit) {
    if (locale == null) {
        content()
        return
    }
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val localized = remember(context, configuration, locale) {
        ContextThemeWrapper(context, context.theme).apply {
            applyOverrideConfiguration(Configuration(configuration).apply { setLocales(LocaleList(locale)) })
        }
    }
    CompositionLocalProvider(
        LocalContext provides localized,
        LocalConfiguration provides localized.resources.configuration,
        content = content,
    )
}
