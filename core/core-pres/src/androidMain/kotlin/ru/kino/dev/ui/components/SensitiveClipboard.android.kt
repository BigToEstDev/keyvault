package ru.kino.dev.ui.components

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.res.stringResource

/**
 * The clipboard of android, the value marked sensitive: from Android 13 the system shows it as dots in its "copied"
 * bubble, and keyboards keep it out of their history and suggestions. Older versions ignore the mark and show no
 * bubble - there the app says [copied] itself.
 */
private class AndroidSensitiveClipboard(
    private val context: Context,
    private val copied: String,
) : SensitiveClipboard {

    override fun copy(label: String, value: String) {
        val clip = ClipData.newPlainText(label, value).apply {
            description.extras = PersistableBundle().apply { putBoolean(EXTRA_IS_SENSITIVE, true) }
        }
        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
        }
    }

    private companion object {
        // ClipDescription.EXTRA_IS_SENSITIVE is there from Android 13; the key is the same string below it
        val EXTRA_IS_SENSITIVE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ClipDescription.EXTRA_IS_SENSITIVE
        } else {
            "android.content.extra.IS_SENSITIVE"
        }
    }
}

@Composable
actual fun rememberSensitiveClipboard(): SensitiveClipboard {
    val context = LocalContext.current
    val copied = stringResource(Res.string.copied)
    return remember(context, copied) { AndroidSensitiveClipboard(context.applicationContext, copied) }
}
