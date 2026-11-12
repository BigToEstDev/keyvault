package ru.kino.dev.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import ru.kino.dev.core.pres.*
import ru.kino.dev.ui.res.stringResource

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

/** The button that opens [link] - one [linkOf] gave - in the browser. */
@Composable
fun OpenLinkButton(link: String) {
    val open = rememberLinkOpener()
    IconButton(onClick = { open(link) }) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = stringResource(Res.string.action_open_link),
        )
    }
}

/** Opens a link of [linkOf] in the browser of the system. */
@Composable
expect fun rememberLinkOpener(): (link: String) -> Unit

private const val SCHEME_END = "://"
private const val HTTPS = "https"
private val WEB = setOf("http", HTTPS)

/** A scheme with no slashes after it: letters first, then a colon and no digit - not a port. */
private val OTHER_SCHEME = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:(?!\\d)")

/** A host with a dot in it, a port and a path after it, if any. */
private val BARE_HOST = Regex("^[\\p{L}\\p{N}-]+(\\.[\\p{L}\\p{N}-]+)+(:\\d+)?([/?#].*)?$")
