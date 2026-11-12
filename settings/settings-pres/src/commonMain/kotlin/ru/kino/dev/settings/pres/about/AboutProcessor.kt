package ru.kino.dev.settings.pres.about

import androidx.lifecycle.ViewModel
import kotlin.coroutines.cancellation.CancellationException
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.settings.about.AppInfo
import ru.kino.dev.settings.about.CoreVersionRepository

/**
 * The screen "About" (pass-docs, Step 122): the app, its version and build, the license, where to write and where the
 * code is. Of the app, not of the open database - no `dbKey`. The version of the core is asked of the core (Step 123).
 */
class AboutProcessor(
    private val info: AppInfo,
    private val coreVersions: CoreVersionRepository,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<AboutState, AboutState, AboutSideEffect>, AboutActions {

    override val container = orbitContainer<AboutState, AboutSideEffect>(AboutState.of(info))

    override fun onLoad() {
        intent {
            val shown = try {
                CoreVersionShown.Known(coreVersions.coreVersion())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                // The screen is still of use: the line says the version is unknown
                log.unexpected(TAG, failure)
                CoreVersionShown.Unknown
            }
            reduce { state.copy(coreVersion = shown) }
        }
    }

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

    private companion object {
        const val TAG = "AboutProcessor"
    }
}
