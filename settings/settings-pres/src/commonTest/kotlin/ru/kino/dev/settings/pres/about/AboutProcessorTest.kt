package ru.kino.dev.settings.pres.about

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.settings.about.AppInfo
import ru.kino.dev.settings.about.BuildType
import ru.kino.dev.settings.pres.testing.recordNavigation
import ru.kino.dev.settings.pres.testing.runToEnd

class AboutProcessorTest {

    private val info = AppInfo(
        version = "1.2.3",
        buildType = BuildType.Release,
        repoUrl = "https://github.com/owner/app/",
        contactEmail = "mail@example.com",
    )

    @Test
    fun `what the app tells of itself is shown at once`() = runTest {
        val state = processor(recordNavigation().first).container.stateFlow.value

        assertEquals(AboutState("1.2.3", BuildType.Release, "mail@example.com", "https://github.com/owner/app/"), state)
        assertEquals("github.com/owner/app", state.repoShown)
    }

    @Test
    fun `back leaves the screen`() = runTest {
        val (navigator, events) = recordNavigation()

        processor(navigator).testWithInternalState(this) { containerHost.runToEnd { onBack() } }

        assertEquals(listOf<MviNavEvent>(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `the email is copied`() = runTest {
        processor(recordNavigation().first).testWithInternalState(this) {
            containerHost.onCopyEmail()

            expectSideEffect(AboutSideEffect.CopyEmail("mail@example.com"))
        }
    }

    @Test
    fun `a bug is reported in the issues of the repository`() = runTest {
        processor(recordNavigation().first).testWithInternalState(this) {
            containerHost.onReportIssue()

            expectSideEffect(AboutSideEffect.OpenLink("https://github.com/owner/app/issues"))
        }
    }

    @Test
    fun `the source code opens the repository`() = runTest {
        processor(recordNavigation().first).testWithInternalState(this) {
            containerHost.onOpenSourceCode()

            expectSideEffect(AboutSideEffect.OpenLink("https://github.com/owner/app/"))
        }
    }

    @Test
    fun `what has no page yet leads nowhere`() = runTest {
        val (navigator, events) = recordNavigation()

        // No side effect, no navigation: the test fails on an effect left unconsumed
        processor(navigator).testWithInternalState(this) {
            containerHost.runToEnd {
                onSupport()
                onOpenPrivacyPolicy()
                onOpenSite()
            }
        }

        assertTrue(events.isEmpty())
    }

    private fun processor(navigator: Navigator) = AboutProcessor(info, navigator)
}
