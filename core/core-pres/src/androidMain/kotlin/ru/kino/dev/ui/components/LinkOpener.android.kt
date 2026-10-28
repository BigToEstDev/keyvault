package ru.kino.dev.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.res.stringResource

// No app to open it - a toast says so, as copying speaks for itself below Android 13
@Composable
internal actual fun rememberLinkOpener(): (link: String) -> Unit {
    val context = LocalContext.current
    val noApp = stringResource(Res.string.message_no_app_for_link)
    return remember(context, noApp) {
        { link ->
            val intent = Intent(Intent.ACTION_VIEW, link.toUri()).addCategory(Intent.CATEGORY_BROWSABLE)
            try {
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, noApp, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
