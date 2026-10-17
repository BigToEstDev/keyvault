package ru.kino.dev.ui.components

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import ru.kino.dev.core.pres.R

/**
 * Puts a value of an entry into the clipboard, marked sensitive: from Android 13 the system shows it as dots
 * in its "copied" bubble, and keyboards keep it out of their history and suggestions. Older versions ignore
 * the mark and show no bubble - there the app says "copied" itself.
 *
 * The clipboard is not cleared afterwards - that comes with locking the database (pass-docs, Step 46).
 */
class SensitiveClipboard internal constructor(private val context: Context) {

    /** Copies [value]; [label] names it for the system - the name of the field, never the value. */
    fun copy(label: String, value: String) {
        val clip = ClipData.newPlainText(label, value).apply {
            description.extras = PersistableBundle().apply { putBoolean(EXTRA_IS_SENSITIVE, true) }
        }
        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
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
fun rememberSensitiveClipboard(): SensitiveClipboard {
    val context = LocalContext.current
    return remember(context) { SensitiveClipboard(context.applicationContext) }
}

/** The button that copies the value of a field. */
@Composable
fun CopyButton(onCopy: () -> Unit, enabled: Boolean = true) {
    IconButton(onClick = onCopy, enabled = enabled) {
        Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.action_copy))
    }
}
