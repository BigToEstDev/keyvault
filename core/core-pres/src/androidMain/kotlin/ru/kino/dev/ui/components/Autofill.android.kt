package ru.kino.dev.ui.components

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.doOnAttach

// The root of the window, not the view of Compose: that one reports itself important for autofill whatever it is
// told (`AndroidComposeView.getImportantForAutofill`), the root above it is asked too. Once the view is in its window
@Composable
internal actual fun ExcludeWindowFromAutofill() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.doOnAttach { it.rootView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS }
        onDispose {}
    }
}
