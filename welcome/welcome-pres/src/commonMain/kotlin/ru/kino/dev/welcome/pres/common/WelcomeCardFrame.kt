package ru.kino.dev.welcome.pres.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kino.dev.ui.components.GradientBackground

/**
 * A screen of `welcome` in a wide window (pass-docs, `docs/app/mvi-arc.md`, Step 94, Step 95): what a phone shows in
 * a column, all of it on a [WelcomeCard] in the middle of the window, the backdrop round it; scrolls when the window
 * is low.
 */
@Composable
internal fun WelcomeCardFrame(content: @Composable ColumnScope.() -> Unit) {
    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            WelcomeCard(
                modifier = Modifier
                    .widthIn(max = CARD_MAX_WIDTH)
                    .fillMaxWidth(),
                content = content,
            )
        }
    }
}

/** The card holds a field and its buttons - wider than this they stretch to no use. */
private val CARD_MAX_WIDTH = 440.dp
