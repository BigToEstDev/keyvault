package ru.kino.dev.core.phone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.kino.dev.core.FieldType

class PhoneTypeTest {

    @Test
    fun `a phone type carries its country after the colon`() {
        assertEquals(FieldType.PHONE, FieldType.of("phone:KZ"))
        assertEquals("KZ", PhoneType.region("phone:KZ"))
        assertEquals("phone:KZ", PhoneType.of("KZ"))
    }

    @Test
    fun `a phone type without a country is a phone of none yet`() {
        assertEquals(FieldType.PHONE, FieldType.of("phone"))
        assertNull(PhoneType.region("phone"))
        assertNull(PhoneType.region("phone:"))
    }

    @Test
    fun `no type, a type this app does not know - text, of no country`() {
        assertEquals(FieldType.TEXT, FieldType.of(null))
        assertEquals(FieldType.TEXT, FieldType.of("card:XX"))
        assertNull(PhoneType.region("card:XX"))
        assertNull(PhoneType.region(null))
    }
}
