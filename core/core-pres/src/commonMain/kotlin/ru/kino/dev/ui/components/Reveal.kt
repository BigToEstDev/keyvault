package ru.kino.dev.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * [content] of [value] that opens out when the value comes and folds away when it goes, so what is under it
 * moves smoothly instead of jumping - a line that is known later than the screen, as the open time of the rounds
 * (pass-docs, Step 72, fix 2). A value there from the start is shown at once, with no animation.
 *
 * Folding away, the last value stays drawn: [value] is null by then.
 */
@Composable
fun <T : Any> Reveal(value: T?, content: @Composable (T) -> Unit) {
    val last = remember { LastValue<T>() }
    value?.let { last.value = it }
    Reveal(visible = value != null) {
        last.value?.let { content(it) }
    }
}

/** [content] that opens out when [visible] and folds away when not - as [Reveal] of a value, with nothing to keep. */
@Composable
fun Reveal(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        content()
    }
}

// Not a state: written while composing, read only by the same composition
private class LastValue<T : Any> {
    var value: T? = null
}
