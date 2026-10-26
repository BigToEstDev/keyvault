package ru.kino.dev.phone

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.kino.dev.core.phone.PhoneCountry

class LibPhoneNumbersTest {

    private val phones = LibPhoneNumbers()

    @Test
    fun `the value kept is E164 - the calling code of the country and the digits`() {
        assertEquals("+79001234567", phones.e164("RU", "9001234567"))
        assertEquals("+77011234567", phones.e164("KZ", "7011234567"))
        assertEquals("", phones.e164("RU", ""))
    }

    @Test
    fun `the digits come back from the value kept, the calling code of the country left out`() {
        assertEquals("9001234567", phones.digits("+79001234567", "RU"))
        assertEquals("612345678", phones.digits("+31612345678", "NL"))
    }

    @Test
    fun `a value not of the calling code of its country gives all its digits`() {
        assertEquals("31612345678", phones.digits("+31612345678", "RU"))
    }

    @Test
    fun `a number is grouped as its country writes it`() {
        assertEquals("900 123-45-67", phones.formatNational("RU", "9001234567"))
    }

    @Test
    fun `a number typed in part is grouped as far as it goes`() {
        assertEquals("900 12", phones.formatNational("RU", "90012").filterNot { it == '-' }.trim())
    }

    @Test
    fun `digits past what the country takes come as they are`() {
        val digits = "90012345678901"

        assertEquals(digits, phones.formatNational("RU", digits).filter(Char::isDigit))
    }

    @Test
    fun `+7 is Russia and Kazakhstan, each a number of its own`() {
        assertEquals(7, phones.callingCode("RU"))
        assertEquals(7, phones.callingCode("KZ"))
        assertTrue(phones.isValid("RU", "9001234567"))
        assertTrue(phones.isValid("KZ", "7011234567"))
        assertFalse(phones.isValid("KZ", "9001234567"))
    }

    @Test
    fun `a number too short is not valid`() {
        assertFalse(phones.isValid("RU", "900123"))
    }

    @Test
    fun `a code there is no country of has no calling code`() {
        assertNull(phones.callingCode("XX"))
        assertNull(phones.callingCode("001"))
    }

    @Test
    fun `the countries go by calling code, then by code, numbers of no country left out`() = runTest {
        val countries = phones.countries()

        assertEquals(PhoneCountry("US", 1), countries.first { it.region == "US" })
        assertEquals(countries.sortedWith(compareBy({ it.callingCode }, { it.region })), countries)
        assertTrue(countries.none { it.region == "001" })
        assertTrue(countries.indexOfFirst { it.region == "KZ" } > countries.indexOfFirst { it.region == "CA" })
    }
}
