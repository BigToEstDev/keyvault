package ru.kino.dev.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import ru.kino.dev.core.pres.R

/**
 * When the error bubble of a field is up.
 *
 * A new error brings it up, focus or not - an error that comes when the field is left, a mismatch with the
 * field above, has to show too. Down once focus leaves the field, up again when it comes back; the "!" icon
 * flips it either way.
 *
 * Focus lost to the field going disabled - the wait while the input is checked - does not count: that wait
 * may end with the same error as before, which is no change of [error] and would not bring the bubble back.
 */
@Stable
internal class FieldErrorState {
    var shown by mutableStateOf(false)
        private set
    private var focused = false

    fun onFocusChanged(focused: Boolean, enabled: Boolean) {
        if (!focused && !enabled) return
        this.focused = focused
        shown = focused
    }

    fun toggle() {
        shown = !shown
    }

    fun onErrorChanged(error: String?) {
        shown = error != null
    }
}

/** A [FieldErrorState] that shows the bubble each time [error] changes to a new one. */
@Composable
internal fun rememberFieldErrorState(error: String?): FieldErrorState {
    val state = remember { FieldErrorState() }
    LaunchedEffect(error) { state.onErrorChanged(error) }
    return state
}

/**
 * The "!" in the trailing slot of a field with an error, and the bubble with the error text it points to.
 *
 * The bubble is a popup under the field, over whatever is below: the layout does not move when an error comes
 * or goes. It does not take focus - the keyboard stays - and does not close on a tap outside: that tap may be
 * the "!" itself, which would close it and open it again.
 *
 * A popup is a window of its own: scrolled with the form, it would not go with the field but stop at the edge of
 * the screen, the bubbles of several fields one over the other. So it is shown only while the "!" is whole in
 * sight; cut by the edge of what scrolls even a little, the bubble goes, the "!" stays - and the bubble comes back
 * with the field (pass-docs, Step 72, item 7).
 */
@Composable
internal fun FieldErrorIcon(error: String, state: FieldErrorState) {
    // Under the icon, the popup reaches as far as the window lets it toward the start of the field
    var maxWidthPx by remember { mutableIntStateOf(0) }
    var inSight by remember { mutableStateOf(true) }
    val layoutDirection = LocalLayoutDirection.current

    Box(
        modifier = Modifier.onGloballyPositioned { coordinates ->
            // Clipped by what scrolls and by every parent: smaller than the button - the field is going out of sight
            val bounds = coordinates.boundsInWindow()
            inSight = bounds.width >= coordinates.size.width - SIGHT_SLACK &&
                bounds.height >= coordinates.size.height - SIGHT_SLACK
            val windowWidth = coordinates.findRootCoordinates().size.width
            val room = if (layoutDirection == LayoutDirection.Ltr) bounds.right else windowWidth - bounds.left
            maxWidthPx = room.toInt()
        },
    ) {
        IconButton(onClick = state::toggle) {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = stringResource(R.string.field_error_show),
                tint = MaterialTheme.colorScheme.error,
            )
        }
        ErrorBubblePopup(error = error, visible = state.shown && inSight, maxWidthPx = maxWidthPx)
    }
}

@Composable
private fun ErrorBubblePopup(error: String, visible: Boolean, maxWidthPx: Int) {
    val transition = remember { MutableTransitionState(false) }
    transition.targetState = visible
    if (!transition.currentState && !transition.targetState) return

    val density = LocalDensity.current
    val position = remember(density) {
        BelowEndPositionProvider(gapPx = with(density) { BubbleGap.roundToPx() })
    }
    val maxWidth = with(density) { maxWidthPx.toDp() } - WindowMargin
    // Grows out of the tail: the top corner on the end side
    val origin = TransformOrigin(
        pivotFractionX = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1f else 0f,
        pivotFractionY = 0f,
    )

    Popup(popupPositionProvider = position, properties = PopupProperties(focusable = false)) {
        AnimatedVisibility(
            visibleState = transition,
            enter = fadeIn() + scaleIn(initialScale = 0.9f, transformOrigin = origin),
            exit = fadeOut() + scaleOut(targetScale = 0.9f, transformOrigin = origin),
        ) {
            ErrorBubble(text = error, maxWidth = maxWidth)
        }
    }
}

/** A rounded box with a tail on top, the tail under the middle of the "!" button. */
@Composable
private fun ErrorBubble(text: String, maxWidth: Dp) {
    val colors = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier
            .widthIn(max = maxWidth.coerceAtLeast(MinBubbleWidth))
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Box(
            Modifier
                .padding(end = TailInsetEnd)
                .size(width = TailWidth, height = TailHeight)
                .background(colors.error, TailShape),
        )
        Text(
            text = text,
            color = colors.onError,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .background(colors.error, MaterialTheme.shapes.medium)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

/** The popup's end edge on the anchor's end edge, its top just under the anchor - the field's border. */
private class BelowEndPositionProvider(private val gapPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = if (layoutDirection == LayoutDirection.Ltr) {
            anchorBounds.right - popupContentSize.width
        } else {
            anchorBounds.left
        }
        return IntOffset(x, anchorBounds.bottom + gapPx)
    }
}

private val TailShape = GenericShape { size, _ ->
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

private val TailWidth = 12.dp
private val TailHeight = 6.dp

// An icon button is 48dp wide: its middle is 24dp from the end, the tail is centered there
private val TailInsetEnd = 24.dp - TailWidth / 2

// The 48dp button sits in the middle of the 56dp field: its bottom is 4dp above the field's border
private val BubbleGap = 4.dp

private val WindowMargin = 16.dp

// The bounds are floats; under a pixel of a cut is no cut
private const val SIGHT_SLACK = 0.5f
private val MinBubbleWidth = 120.dp
