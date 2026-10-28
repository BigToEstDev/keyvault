package ru.kino.dev.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

/**
 * The desktop entry point. An empty window for now: the screens come with `core-pres` and the feature
 * modules (Step 87-88 cmp, pass-docs); until then it only shows whether the Rust bridge loaded.
 *
 * The texts are not resources on purpose: `Res` of `core-pres` arrives in Step 87, this screen goes away
 * with it.
 */
fun main() {
    val bridge = PassFfiLibrary.load()

    application {
        Window(onCloseRequest = ::exitApplication, title = "KeyVault") {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = bridge.fold(
                                onSuccess = { "pass_ffi loaded: ${it.absolutePath}" },
                                onFailure = { "pass_ffi failed to load: ${it.message}" },
                            ),
                        )
                    }
                }
            }
        }
    }
}
