package ru.kino.dev.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.koin.core.context.startKoin
import ru.kino.dev.app.App
import ru.kino.dev.di.appModules
import ru.kino.dev.di.coreDataModule
import ru.kino.dev.settings.lock.InactivityTimer

/**
 * The desktop app: the window and the graph around the app common to the platforms (pass-docs, Step 88). The window
 * is of the size of a phone for now - the layout for a window is Step 89.
 */
fun main() {
    val koin = startKoin { modules(appModules + coreDataModule) }.koin
    // Told what the app sees; `vault` listens to it (pass-docs, Step 59). Leaving - the window put away - is Step 89
    val timer = koin.get<InactivityTimer>()
    timer.cameBack()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "KeyVault",
            state = rememberWindowState(width = 420.dp, height = 860.dp),
            // Every key the window takes - the person is here; the screens get it as before
            onPreviewKeyEvent = {
                timer.touched()
                false
            },
        ) {
            // Every press, whatever takes it, seen before it does and left to it: a press counts once, as it starts
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(timer) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.type == PointerEventType.Press) timer.touched()
                            }
                        }
                    },
            ) {
                App()
            }
        }
    }
}
