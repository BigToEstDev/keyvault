package ru.kino.dev.settings.pres.about

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.settings.about.AppInfo

/**
 * The screen "About" (pass-docs, Step 122): the app, its version and build, the license, where to write and where the
 * code is. Of the app, not of the open database - no `dbKey`.
 */
class AboutProcessor(
    private val info: AppInfo,
    private val navigator: Navigator,
) : ViewModel(), OrbitContainerHost<AboutState, AboutState, AboutSideEffect>, AboutActions {

    override val container = orbitContainer<AboutState, AboutSideEffect>(AboutState.of(info))

    override fun onBack() {
        intent { navigator.navigate(MviNavEvent.NavigateBack) }
    }

    // No page of donations yet: the button is there, it leads nowhere (pass-docs, Step 122)
    override fun onSupport() = Unit

    // No page of the policy on the site yet (pass-docs, Step 122)
    override fun onOpenPrivacyPolicy() = Unit

    // The site is not published yet (pass-docs, Step 122)
    override fun onOpenSite() = Unit

    override fun onCopyEmail() {
        intent { postSideEffect(AboutSideEffect.CopyEmail(info.contactEmail)) }
    }

    override fun onReportIssue() {
        intent { postSideEffect(AboutSideEffect.OpenLink(info.issuesUrl)) }
    }

    override fun onOpenSourceCode() {
        intent { postSideEffect(AboutSideEffect.OpenLink(info.repoUrl)) }
    }
}
