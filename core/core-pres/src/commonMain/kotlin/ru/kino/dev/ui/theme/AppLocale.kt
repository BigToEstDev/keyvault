package ru.kino.dev.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import java.util.Locale

/**
 * The language the app shows its strings in now. Read by the strings of `ui.res` - so that they are read again when
 * it changes - and by what names things in the language of the app, as the countries of the phone field.
 */
val LocalAppLocale = compositionLocalOf<Locale> { Locale.getDefault() }

/**
 * [content] in [locale], without building it anew - the screen, the open database and the back stack stay as they
 * are (pass-docs, Step 57). Null - the language of the system.
 *
 * Compose resources take the language from the default locale of the process, so that is what is set: the language
 * chosen, the country of the system - the prefix of the phone field is the country of the device, not of the
 * language (pass-docs, Step 77). Dates and numbers follow the language of the app with it (pass-docs, Step 87).
 */
@Composable
fun AppLocale(locale: Locale?, content: @Composable () -> Unit) {
    val shown = remember(locale) { applyLocale(locale) }
    CompositionLocalProvider(LocalAppLocale provides shown) {
        PlatformLocale(locale, content)
    }
}

private fun applyLocale(locale: Locale?): Locale {
    val system = systemLocale()
    val shown = locale?.let { Locale.Builder().setLanguage(it.language).setRegion(system.country).build() } ?: system
    Locale.setDefault(shown)
    return shown
}

/** What the platform localizes beyond Compose resources - the resources of android; nothing on the desktop. */
@Composable
internal expect fun PlatformLocale(locale: Locale?, content: @Composable () -> Unit)

/** The language and the country of the system, whatever the app set. */
internal expect fun systemLocale(): Locale
