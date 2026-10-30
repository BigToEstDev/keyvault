package ru.kino.dev.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
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
 * is of the size of a phone for now - the layout for a window is Step 90.
 */
fun main() {
    val koin = startKoin { modules(appModules + coreDataModule) }.koin
    // Told what the app sees; `vault` listens to it (pass-docs, Step 59, Step 89)
    val timer = koin.get<InactivityTimer>()

    application {
        val windowState = rememberWindowState(width = 420.dp, height = 860.dp)

        // Put away - leaving, as on android; shown again - coming back, the first time too. The focus lost is not
        // leaving: the secret copied is taken to another window
        LaunchedEffect(windowState) {
            snapshotFlow { windowState.isMinimized }.collect { minimized ->
                if (minimized) timer.wentAway() else timer.cameBack()
            }
        }
        LaunchedEffect(timer) { SleepWatch(timer).watch() }

        Window(
            onCloseRequest = ::exitApplication,
            title = "KeyVault",
            state = windowState,
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
