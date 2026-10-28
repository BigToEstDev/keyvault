package ru.kino.dev.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import ru.kino.dev.ui.components.PasswordStrength.Medium
import ru.kino.dev.ui.components.PasswordStrength.Strong
import ru.kino.dev.ui.components.PasswordStrength.Weak

class PasswordStrengthTest {

    @Test
    fun `an empty password has no strength`() {
        assertNull(passwordStrength(""))
    }

    @Test
    fun `the alphabet is the sum of the kinds the password has`() {
        assertEquals(26, alphabetOf("abc"))
        assertEquals(36, alphabetOf("qwerty12"))
        assertEquals(95, alphabetOf("Ab1!"))
        assertEquals(33 + 33, alphabetOf("Пароль"))
        assertEquals(26 + 100, alphabetOf("passé"))
    }

    @Test
    fun `short and of one kind is weak`() {
        assertEquals(Weak, passwordStrength("password"))
        assertEquals(Weak, passwordStrength("пароль"))
    }

    @Test
    fun `longer or of more kinds is medium`() {
        assertEquals(Medium, passwordStrength("qwerty12"))
        assertEquals(Medium, passwordStrength("Abc123!@xyz"))
    }

    @Test
    fun `long and of every kind is strong`() {
        assertEquals(Strong, passwordStrength("k9#Vq2!xR7m\$Lp4@"))
    }

    @Test
    fun `40 bits is where medium starts`() {
        // 12 digits - 39.9 bits, 13 - 43.2
        assertEquals(Weak, passwordStrength("123456789012"))
        assertEquals(Medium, passwordStrength("1234567890123"))
    }

    @Test
    fun `80 bits is where strong starts`() {
        // Four kinds, 95 characters: 12 of them - 78.8 bits, 13 - 85.4
        assertEquals(Medium, passwordStrength("Abcdef12!@#x"))
        assertEquals(Strong, passwordStrength("Abcdef12!@#xy"))
    }
}
