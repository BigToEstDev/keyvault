package ru.kino.dev.pane

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import ru.kino.dev.ui.theme.LayoutType
import ru.kino.dev.ui.theme.LocalLayoutType

/**
 * A place of the graph drawn by the width of the window: [mobile] - a screen, [tablet] - a container of several
 * (pass-docs, `docs/dev/navigation.md`). One route for both, so the back stack is the same either way.
 */
inline fun <reified T : Any> NavGraphBuilder.adaptiveComposable(
    noinline mobile: @Composable (NavBackStackEntry) -> Unit,
    noinline tablet: @Composable (NavBackStackEntry) -> Unit,
) {
    composable<T> { entry ->
        when (LocalLayoutType.current) {
            LayoutType.Mobile -> mobile(entry)
            LayoutType.Tablet -> tablet(entry)
        }
    }
}
