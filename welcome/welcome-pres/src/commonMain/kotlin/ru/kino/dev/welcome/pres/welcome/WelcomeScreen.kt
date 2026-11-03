package ru.kino.dev.welcome.pres.welcome

import androidx.compose.runtime.Composable
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.kino.dev.pane.koinProcessor
import ru.kino.dev.ui.files.DocumentKind
import ru.kino.dev.ui.files.rememberOpenDocument

@Composable
internal fun WelcomeScreen(processor: WelcomeProcessor = koinProcessor()) {
    val pickDatabase = rememberOpenDocument(DocumentKind.Database) { processor.onDatabasePicked(it) }

    processor.collectSideEffect { effect ->
        when (effect) {
            WelcomeSideEffect.PickDatabase -> pickDatabase()
        }
    }

    processor.WelcomeContent()
}
