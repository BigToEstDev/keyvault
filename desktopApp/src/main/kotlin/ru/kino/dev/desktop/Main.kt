package ru.kino.dev.desktop

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.koin.core.context.startKoin
import ru.kino.dev.app.App
import ru.kino.dev.di.appModules
import ru.kino.dev.di.coreDataModule
import ru.kino.dev.settings.lock.InactivityTimer
import ru.kino.dev.vault.sync.AppReturns
import ru.kino.dev.ui.components.appIcon
import ru.kino.dev.ui.files.LocalPickerParent

/**
 * The desktop app: the window and the graph around the app common to the platforms (pass-docs, Step 88). The window
 * opens made for the desktop ([WindowSizes], Step 90).
 */
fun main() {
    TitleBar.followSystemOnMac()
    val koin = startKoin { modules(appModules + coreDataModule) }.koin
    // Told what the app sees; `vault` listens to it (pass-docs, Step 59, Step 89)
    val timer = koin.get<InactivityTimer>()
    // Coming back - a file written elsewhere meanwhile is merged quietly (pass-docs, Step 108)
    val returns = koin.get<AppReturns>()

    application {
        val windowState = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            size = WindowSizes.initial(WindowSizes.screen()),
        )

        // Put away - leaving, as on android; shown again - coming back, the first time too. The focus lost is not
        // leaving: the secret copied is taken to another window
        LaunchedEffect(windowState) {
            snapshotFlow { windowState.isMinimized }.collect { minimized ->
                if (minimized) {
                    timer.wentAway()
                } else {
                    timer.cameBack()
                    returns.cameBack()
                }
            }
        }
        LaunchedEffect(timer) { SleepWatch(timer).watch() }

        Window(
            onCloseRequest = ::exitApplication,
            title = "KeyVault",
            icon = rememberVectorPainter(remember { appIcon() }),
            state = windowState,
            // Every key the window takes - the person is here; the screens get it as before
            onPreviewKeyEvent = {
                timer.touched()
                false
            },
        ) {
            LaunchedEffect(window) { window.minimumSize = WindowSizes.minAwt }
            val darkSystem = isSystemInDarkTheme()
            LaunchedEffect(window, darkSystem) { TitleBar.applyDark(window, darkSystem) }

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
                // The pickers of files open over this window
                CompositionLocalProvider(LocalPickerParent provides window) {
                    App()
                }
            }
        }
    }
}
