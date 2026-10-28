package ru.kino.dev.ui.theme

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import android.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * The android part of [AppLocale]: the resources of the context and the configuration in [locale] too - for what
 * reads them rather than Compose resources: the strings of Material 3 and the system (pass-docs, Step 57, 87).
 *
 * The context wraps the activity rather than being a new configuration context: `BiometricPrompt` and the like
 * look for the activity through it. [content] sits under the same provider whatever the [locale], the system one
 * too: a branch without it would be another tree for Compose - switching to or from the system language would build
 * the navigation anew, at its start (pass-docs, Step 57).
 */
// Lint sees this module alone: the bundle keeps every language in its base - `app/build.gradle.kts`, `bundle`
@SuppressLint("AppBundleLocaleChanges")
@Composable
internal actual fun PlatformLocale(locale: Locale?, content: @Composable () -> Unit) {
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

// The language of the system, not of the app: the resources of the system are not touched by AppLocale
internal actual fun systemLocale(): Locale = Resources.getSystem().configuration.locales[0]
