package ru.kino.dev.ui.theme

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import java.util.Locale

/**
 * The resources of the activity in the language of the app - for what [AppLocale] does not reach: a `Dialog`,
 * a `Popup` and the menus on them are windows of their own, built on the context of the activity rather than
 * on the composition's, so they took the language of the system (pass-docs, Step 72, fix 1).
 *
 * The activity returns [of] from `getResources()`. [locale] is set as soon as the language is known; null - the
 * language of the system, the resources are given back as they are. The localized ones are built once for a
 * locale and a configuration: a turn of the screen, the theme or the size of the font build them anew.
 */
// Lint sees this module alone: the bundle keeps every language in its base - `app/build.gradle.kts`, `bundle`
@SuppressLint("AppBundleLocaleChanges")
class LocaleResources {

    @Volatile
    var locale: Locale? = null

    private var built: Built? = null

    /**
     * [base] in [locale]. [context] builds them - the base context of the activity, not the activity: its
     * `getResources()` is what asks.
     */
    fun of(context: Context, base: Resources): Resources {
        val locale = locale ?: return base
        val configuration = base.configuration
        built?.takeIf { it.locale == locale && it.configuration == configuration }?.let { return it.resources }
        val localized = Configuration(configuration).apply { setLocales(LocaleList(locale)) }
        val resources = context.createConfigurationContext(localized).resources
        // A copy: the configuration of the base resources changes in place
        built = Built(locale, Configuration(configuration), resources)
        return resources
    }

    private class Built(val locale: Locale, val configuration: Configuration, val resources: Resources)
}
