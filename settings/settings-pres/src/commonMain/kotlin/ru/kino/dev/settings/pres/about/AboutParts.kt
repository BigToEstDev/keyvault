package ru.kino.dev.settings.pres.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.kino.dev.core.pres.*
import ru.kino.dev.settings.about.BuildType
import ru.kino.dev.settings.pres.about.components.AboutRow
import ru.kino.dev.ui.components.SkeletonText
import ru.kino.dev.ui.components.appIcon
import ru.kino.dev.ui.components.skeletonBrush
import ru.kino.dev.ui.res.stringResource

/** The app: its icon on the left; its name, version and build on the right; what it is - under them. */
@Composable
internal fun AppCard(state: AboutState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    imageVector = remember { appIcon() },
                    contentDescription = null,
                    modifier = Modifier.size(APP_ICON_SIZE),
                )
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(text = stringResource(Res.string.app_name), style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = stringResource(Res.string.about_version, state.version),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(
                            when (state.buildType) {
                                BuildType.Debug -> Res.string.about_build_debug
                                BuildType.Release -> Res.string.about_build_release
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    CoreVersionLine(state.coreVersion)
                }
            }
            Text(
                text = stringResource(Res.string.about_description),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** The version of the core under the build; a skeleton while it is asked (pass-docs, Step 123). */
@Composable
private fun CoreVersionLine(shown: CoreVersionShown) {
    val style = MaterialTheme.typography.bodySmall
    when (shown) {
        CoreVersionShown.Loading -> SkeletonText(
            brush = skeletonBrush(),
            style = style,
            modifier = Modifier.width(CORE_SKELETON_WIDTH),
        )
        is CoreVersionShown.Known -> Text(
            text = stringResource(Res.string.about_core_version, shown.version),
            style = style,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CoreVersionShown.Unknown -> Text(
            text = stringResource(Res.string.about_core_version_unknown),
            style = style,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Free, under GPL-3.0, as is; the support of the project - gold, as on the site (pass-docs, Step 122). */
@Composable
internal fun AboutActions.SupportCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(SUPPORT_BADGE_SIZE)
                    .background(MaterialTheme.colorScheme.tertiary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.LocalCafe, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiary)
            }
            Text(
                text = stringResource(Res.string.about_free_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(Res.string.about_license),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(Res.string.about_support_text),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = ::onSupport,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
            ) {
                Icon(Icons.Filled.LocalCafe, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Text(text = stringResource(Res.string.about_support), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

/** Collects nothing - and the privacy policy behind it. */
@Composable
internal fun AboutActions.PrivacyCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Filled.VisibilityOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(text = stringResource(Res.string.about_privacy_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(Res.string.about_privacy_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider()
        AboutRow(
            icon = Icons.Filled.Policy,
            title = stringResource(Res.string.about_privacy_policy),
            action = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            onClick = ::onOpenPrivacyPolicy,
        )
    }
}

/** Where to write: the address - copied, the site, the bugs - to the issues. */
@Composable
internal fun AboutActions.ContactsCard(state: AboutState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        AboutRow(
            icon = Icons.Filled.Email,
            title = stringResource(Res.string.about_email),
            text = state.email,
            action = Icons.Filled.ContentCopy,
            onClick = ::onCopyEmail,
        )
        HorizontalDivider()
        AboutRow(
            icon = Icons.Filled.Language,
            title = stringResource(Res.string.about_site),
            text = stringResource(Res.string.about_site_soon),
            action = Icons.AutoMirrored.Filled.OpenInNew,
            onClick = ::onOpenSite,
        )
        HorizontalDivider()
        AboutRow(
            icon = Icons.Filled.BugReport,
            title = stringResource(Res.string.about_report_issue),
            text = stringResource(Res.string.about_report_issue_text),
            action = Icons.AutoMirrored.Filled.OpenInNew,
            onClick = ::onReportIssue,
        )
    }
}

/** The repository of the app - in the browser. */
@Composable
internal fun AboutActions.SourceCodeCard(state: AboutState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        AboutRow(
            icon = Icons.Filled.Code,
            title = stringResource(Res.string.about_source_code),
            text = state.repoShown,
            action = Icons.AutoMirrored.Filled.OpenInNew,
            onClick = ::onOpenSourceCode,
        )
    }
}

private val APP_ICON_SIZE = 72.dp
private val SUPPORT_BADGE_SIZE = 56.dp
private val CORE_SKELETON_WIDTH = 96.dp
