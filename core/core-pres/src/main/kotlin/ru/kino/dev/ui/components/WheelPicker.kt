package ru.kino.dev.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.kino.dev.core.pres.R
import kotlin.math.abs

/**
 * One of [options] on a wheel that spins: the one in the middle, between the two lines, is chosen. It snaps to
 * a choice when let go; a tap on a choice turns the wheel to it. [onSelect] - once the wheel stops on another one.
 *
 * Without state of its own: [selected] is where it starts.
 *
 * @param label the text of an option, from the resources
 */
@Composable
fun <T> WheelPicker(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The first option in the middle: empty rows above it, as many below the last one
    val start = options.indexOf(selected).coerceAtLeast(0)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = start)
    val fling = rememberSnapFlingBehavior(lazyListState = state, snapPosition = SnapPosition.Center)
    val scope = rememberCoroutineScope()

    // The option nearest to the middle of the wheel
    val centered by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val middle = (info.viewportStartOffset + info.viewportEndOffset) / 2
            val nearest = info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - middle) }
            ((nearest?.index ?: (start + SIDE_ROWS)) - SIDE_ROWS).coerceIn(options.indices)
        }
    }
    LaunchedEffect(state, options) {
        snapshotFlow { state.isScrollInProgress to centered }
            .filter { (scrolling, _) -> !scrolling }
            .map { (_, index) -> index }
            .distinctUntilChanged()
            .collect { onSelect(options[it]) }
    }

    Box(modifier = modifier.height(ROW_HEIGHT * (SIDE_ROWS * 2 + 1)), contentAlignment = Alignment.Center) {
        LazyColumn(state = state, flingBehavior = fling, modifier = Modifier.fillMaxWidth()) {
            items(SIDE_ROWS) { Box(modifier = Modifier.height(ROW_HEIGHT)) }
            items(options.size) { index ->
                val chosen = index == centered
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ROW_HEIGHT)
                        // Its row lies SIDE_ROWS below the top one: the row at [index] on top puts it in the middle
                        .clickable { scope.launch { state.animateScrollToItem(index) } },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label(options[index]),
                        style = if (chosen) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                        color = if (chosen) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AWAY_ALPHA)
                        },
                        textAlign = TextAlign.Center,
                    )
                }
            }
            items(SIDE_ROWS) { Box(modifier = Modifier.height(ROW_HEIGHT)) }
        }
        // The lines around the middle - where the choice is
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.primary)
            Box(modifier = Modifier.height(ROW_HEIGHT))
            HorizontalDivider(color = MaterialTheme.colorScheme.primary)
        }
    }
}

/**
 * A choice made on a [WheelPicker] in a dialog: [onConfirm] takes the one the wheel stopped on, "Cancel"
 * leaves it as it was.
 */
@Composable
fun <T> WheelPickerDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onConfirm: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    var choice by remember { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            WheelPicker(
                options = options,
                selected = selected,
                label = label,
                onSelect = { choice = it },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(choice) }) { Text(stringResource(R.string.action_ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private val ROW_HEIGHT = 48.dp

/** Rows above and below the middle one. */
private const val SIDE_ROWS = 2

/** The options away from the middle, faded. */
private const val AWAY_ALPHA = 0.6f
