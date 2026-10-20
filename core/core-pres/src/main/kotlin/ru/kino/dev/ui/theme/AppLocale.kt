package ru.kino.dev.ui.theme

import android.annotation.SuppressLint
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
 *
 * [content] sits under the same provider whatever the [locale], the system one too: a branch without it
 * would be another tree for Compose - switching to or from the system language would build the navigation
 * anew, at its start (pass-docs, Step 57).
 */
// Lint sees this module alone: the bundle keeps every language in its base - `app/build.gradle.kts`, `bundle`
@SuppressLint("AppBundleLocaleChanges")
@Composable
fun AppLocale(locale: Locale?, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val localized = remember(context, configuration, locale) {
        locale?.let {
            ContextThemeWrapper(context, context.theme).apply {
                applyOverrideConfiguration(Configuration(configuration).apply { setLocales(LocaleList(it)) })
            }
        }
    }
    CompositionLocalProvider(
        LocalContext provides (localized ?: context),
        LocalConfiguration provides (localized?.resources?.configuration ?: configuration),
        content = content,
    )
}
