package ru.kino.dev.pane

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ru.kino.dev.navigation.LocalNavigator

/**
 * The processor of a screen: [params] - what the screen gives it, and the navigator of the screen last
 * ([LocalNavigator]: the app's, or a container's inside one). Never the navigator of the graph - inside a container
 * the processor would navigate past it (pass-docs, `docs/dev/navigation.md`, Step 98).
 */
@Composable
inline fun <reified P : ViewModel> koinProcessor(vararg params: Any?, key: String? = null): P {
    val navigator = LocalNavigator.current
    return koinViewModel(key = key) { parametersOf(*params, navigator) }
}
