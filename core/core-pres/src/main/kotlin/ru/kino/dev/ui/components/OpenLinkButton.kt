package ru.kino.dev.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import ru.kino.dev.core.pres.R

/**
 * The web address [value] opens, or null when it opens none (pass-docs, Step 73, item 7). Only the web: `http` and
 * `https` as they are, a bare host - `mail.google.com/inbox` - under `https`. Any other scheme - `cmd://` of
 * KeePass, `file://`, `intent://`, `javascript:` - and a placeholder (`{USERNAME}`) open nothing: an entry of
 * someone else's database must not start whatever it names.
 */
fun linkOf(value: String): String? {
    val text = value.trim()
    if (text.isEmpty() || text.any { it.isWhitespace() || it == '{' }) return null
    val scheme = text.substringBefore(SCHEME_END, missingDelimiterValue = "")
    return when {
        // The scheme in lower case: apps match it as it is written - "HTTPS" finds no browser
        text.contains(SCHEME_END) -> (scheme.lowercase() + text.substring(scheme.length))
            .takeIf { scheme.lowercase() in WEB && text.length > scheme.length + SCHEME_END.length }
        // "mailto:", "javascript:" - a scheme without the slashes; "host:8080" is not one - digits follow it
        OTHER_SCHEME.containsMatchIn(text) -> null
        BARE_HOST.matches(text) -> "$HTTPS$SCHEME_END$text"
        else -> null
    }
}

/**
 * The button that opens [link] - one [linkOf] gave - in the browser. No app to open it - a toast says so, as
 * copying speaks for itself below Android 13.
 */
@Composable
fun OpenLinkButton(link: String) {
    val context = LocalContext.current
    IconButton(onClick = { open(context, link) }) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = stringResource(R.string.action_open_link),
        )
    }
}

private fun open(context: Context, link: String) {
    val intent = Intent(Intent.ACTION_VIEW, link.toUri()).addCategory(Intent.CATEGORY_BROWSABLE)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.message_no_app_for_link, Toast.LENGTH_SHORT).show()
    }
}

private const val SCHEME_END = "://"
private const val HTTPS = "https"
private val WEB = setOf("http", HTTPS)

/** A scheme with no slashes after it: letters first, then a colon and no digit - not a port. */
private val OTHER_SCHEME = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:(?!\\d)")

/** A host with a dot in it, a port and a path after it, if any. */
private val BARE_HOST = Regex("^[\\p{L}\\p{N}-]+(\\.[\\p{L}\\p{N}-]+)+(:\\d+)?([/?#].*)?$")
