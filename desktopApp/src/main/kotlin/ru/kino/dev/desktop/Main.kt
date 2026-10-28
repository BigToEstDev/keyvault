package ru.kino.dev.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.koin.core.context.startKoin
import ru.kino.dev.core.NativeCore
import ru.kino.dev.di.coreDataModule

/**
 * The desktop entry point. No screens yet - they come with `core-pres` and the feature modules (Step 87-88 cmp,
 * pass-docs); until then the window asks the core for its build through the graph, the first real call of the
 * core on the desktop (Step 86).
 *
 * The texts are not resources on purpose: `Res` of `core-pres` arrives in Step 87, this screen goes away with it.
 */
fun main() {
    val koin = startKoin { modules(coreDataModule) }.koin
    val core = koin.get<NativeCore>()

    application {
        Window(onCloseRequest = ::exitApplication, title = "KeyVault") {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // null while the core answers
                    val build by produceState<Result<String>?>(initialValue = null) {
                        value = runCatching { core.buildInfo() }
                    }
                    Box(contentAlignment = Alignment.Center) {
                        when (val shown = build) {
                            null -> CircularProgressIndicator()
                            else -> Text(
                                text = shown.fold(
                                    onSuccess = { "pass_ffi: $it" },
                                    onFailure = { "pass_ffi failed: ${it.message}" },
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
