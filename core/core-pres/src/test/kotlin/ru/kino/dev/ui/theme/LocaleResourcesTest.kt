package ru.kino.dev.ui.theme

import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.kino.dev.core.pres.R
import java.util.Locale

// The system in Russian, the app in English - the case the windows got wrong (pass-docs, Step 72, fix 1)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ru")
class LocaleResourcesTest {

    private val context = RuntimeEnvironment.getApplication()
    private val base = context.resources
    private val resources = LocaleResources()

    @Test
    fun `the language of the system gives the resources back as they are`() {
        assertSame(base, resources.of(context, base))
    }

    @Test
    fun `a language of the app gives the strings in it`() {
        resources.locale = Locale.ENGLISH

        assertEquals("Close", resources.of(context, base).getString(R.string.action_close))
    }

    @Test
    fun `the same language and configuration are built once`() {
        resources.locale = Locale.ENGLISH

        assertSame(resources.of(context, base), resources.of(context, base))
    }

    @Test
    fun `another language is built anew`() {
        resources.locale = Locale.ENGLISH
        resources.of(context, base)
        resources.locale = Locale.forLanguageTag("ru")

        assertEquals("Закрыть", resources.of(context, base).getString(R.string.action_close))
    }

    @Test
    fun `a new configuration is built anew and keeps the language`() {
        resources.locale = Locale.ENGLISH
        val before = resources.of(context, base)

        RuntimeEnvironment.setQualifiers("+night")
        val after = resources.of(context, base)

        assertNotSame(before, after)
        assertEquals(Configuration.UI_MODE_NIGHT_YES, after.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
        assertEquals("Close", after.getString(R.string.action_close))
    }
}
