package ru.kino.dev.ui.res

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import ru.kino.dev.ui.theme.LocalAppLocale
import org.jetbrains.compose.resources.pluralStringResource as composePluralStringResource
import org.jetbrains.compose.resources.stringResource as composeStringResource

/*
 * The strings of the app in its language. Every screen takes its strings through these, not through the ones of
 * Compose resources (pass-docs, Step 87).
 *
 * Compose resources read the language from the default locale of the process, which `AppLocale` sets - but a
 * string read before is not read again until its place is drawn anew. Reading [LocalAppLocale] here makes every
 * place with a string redraw when the language changes, and only those: the tree - the navigation, the screens,
 * what is typed in them - stays as it is (pass-docs, Step 57).
 */

@Composable
fun stringResource(resource: StringResource): String {
    LocalAppLocale.current
    return composeStringResource(resource)
}

@Composable
fun stringResource(resource: StringResource, vararg formatArgs: Any): String {
    LocalAppLocale.current
    return composeStringResource(resource, *formatArgs)
}

@Composable
fun pluralStringResource(resource: PluralStringResource, quantity: Int): String {
    LocalAppLocale.current
    return composePluralStringResource(resource, quantity)
}

@Composable
fun pluralStringResource(resource: PluralStringResource, quantity: Int, vararg formatArgs: Any): String {
    LocalAppLocale.current
    return composePluralStringResource(resource, quantity, *formatArgs)
}
