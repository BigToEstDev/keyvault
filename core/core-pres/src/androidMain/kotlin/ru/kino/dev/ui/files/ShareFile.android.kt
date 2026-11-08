package ru.kino.dev.ui.files

import android.content.ClipData
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri

/** Plain bytes: a kdbx has no mime type apps agree on. */
private const val BINARY_FILE = "application/octet-stream"

/**
 * `ACTION_SEND` through the chooser of the system: the app picked gets a grant to read the `content:` uri of the
 * `FileProvider` - the clip data carries the grant through the chooser to it.
 */
@Composable
actual fun rememberShareFile(): (uri: String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { uri ->
            val file = uri.toUri()
            val send = Intent(Intent.ACTION_SEND)
                .setType(BINARY_FILE)
                .putExtra(Intent.EXTRA_STREAM, file)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            send.clipData = ClipData.newRawUri(null, file)
            // The chooser draws its own title; with no app to pick it says so itself
            context.startActivity(Intent.createChooser(send, null))
        }
    }
}
