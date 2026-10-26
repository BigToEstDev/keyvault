package ru.kino.dev.phone

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.kino.dev.core.phone.PhoneSettingsRepository

class SettingsPhoneDefaultsTest {

    private val phones = LibPhoneNumbers()

    @Test
    fun `the prefix remembered is the country chosen`() = runTest {
        assertEquals("KZ", defaults(remember = true, chosen = "KZ", device = "NL").region())
    }

    @Test
    fun `not remembered - the country of the device, the one chosen aside`() = runTest {
        assertEquals("NL", defaults(remember = false, chosen = "KZ", device = "nl").region())
    }

    @Test
    fun `remembered with none chosen - the country of the device`() = runTest {
        assertEquals("NL", defaults(remember = true, chosen = null, device = "NL").region())
    }

    @Test
    fun `a device of no country of numbers - the main country of the first calling code`() = runTest {
        assertEquals("US", defaults(remember = false, chosen = null, device = "").region())
        assertEquals("US", defaults(remember = false, chosen = null, device = "AQ").region())
    }

    private fun defaults(remember: Boolean, chosen: String?, device: String) =
        SettingsPhoneDefaults(Settings(remember, chosen), phones) { device }

    private class Settings(remember: Boolean, chosen: String?) : PhoneSettingsRepository {
        override val rememberPrefix: Flow<Boolean> = flowOf(remember)
        override val prefixRegion: Flow<String?> = flowOf(chosen)
        override suspend fun saveRememberPrefix(on: Boolean) = Unit
        override suspend fun savePrefixRegion(region: String) = Unit
    }
}
