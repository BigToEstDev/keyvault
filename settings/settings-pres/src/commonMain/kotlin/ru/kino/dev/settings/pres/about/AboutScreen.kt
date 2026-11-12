package ru.kino.dev.settings.pres.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.kino.dev.core.pres.*
import ru.kino.dev.pane.koinProcessor
import ru.kino.dev.settings.pres.settings.components.SettingsSectionHeader
import ru.kino.dev.ui.components.BackTopBar
import ru.kino.dev.ui.components.GradientBackground
import ru.kino.dev.ui.components.rememberLinkOpener
import ru.kino.dev.ui.components.rememberSensitiveClipboard
import ru.kino.dev.ui.res.stringResource

/**
 * About the app (pass-docs, Step 122): the app and its build, free under GPL-3.0 with the support of the project,
 * the privacy, where to write, the source code. Not a landing - a few cards. On a wide window - a page of the container
 * of the settings; the column is not stretched.
 *
 * @param canGoBack false - a page of the container of a wide window: its tab closes it, not "back" (Step 101)
 */
@Composable
internal fun AboutScreen(
    canGoBack: Boolean = true,
    processor: AboutProcessor = koinProcessor(),
) {
    val state by processor.collectAsState()
    val openLink = rememberLinkOpener()
    val clipboard = rememberSensitiveClipboard()
    val emailLabel = stringResource(Res.string.about_email)

    processor.collectSideEffect { effect ->
        when (effect) {
            is AboutSideEffect.OpenLink -> openLink(effect.link)
            is AboutSideEffect.CopyEmail -> clipboard.copy(emailLabel, effect.email)
        }
    }

    GradientBackground {
        Scaffold(
            // The backdrop is the gradient behind
            containerColor = Color.Transparent,
            topBar = {
                BackTopBar(
                    title = stringResource(Res.string.settings_about),
                    onBack = processor::onBack,
                    back = canGoBack,
                )
            },
        ) { padding ->
            processor.AboutContent(state, PaddingValues(16.dp), Modifier.padding(padding))
        }
    }
}

@Composable
private fun AboutActions.AboutContent(state: AboutState, contentPadding: PaddingValues, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val column = Modifier
            .widthIn(max = MAX_WIDTH)
            .fillMaxWidth()
        item(key = "app") { Column(column) { AppCard(state) } }
        item(key = "support") { Column(column) { SupportCard() } }
        item(key = "privacy") { Column(column) { PrivacyCard() } }
        item(key = "contacts-header") {
            Column(column) {
                SettingsSectionHeader(title = stringResource(Res.string.about_contacts), top = 8.dp)
            }
        }
        item(key = "contacts") { Column(column) { ContactsCard(state) } }
        item(key = "source-header") {
            Column(column) {
                SettingsSectionHeader(title = stringResource(Res.string.about_open_source), top = 8.dp)
            }
        }
        item(key = "source") { Column(column) { SourceCodeCard(state) } }
    }
}

/** The column on a wide window: the cards read as on a phone, not stretched across. */
private val MAX_WIDTH = 640.dp
