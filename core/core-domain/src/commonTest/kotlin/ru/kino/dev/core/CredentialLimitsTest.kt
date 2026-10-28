package ru.kino.dev.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The limits as the core counts them.
 *
 * The values themselves are pinned against the core's: a test here that fails after a change of
 * `credential_limits.rs` is the reminder that the two have to move together.
 */
class CredentialLimitsTest {

    @Test
    fun `the limits are the core's`() {
        assertEquals(10L * 1024 * 1024, CredentialLimits.KEY_FILE_MAX_SIZE)
        assertEquals(256, CredentialLimits.PASSWORD_MAX_CHARS)
    }

    @Test
    fun `a key file of the limit is accepted and one byte more is not`() {
        assertFalse(CredentialLimits.isKeyFileTooLarge(CredentialLimits.KEY_FILE_MAX_SIZE))
        assertTrue(CredentialLimits.isKeyFileTooLarge(CredentialLimits.KEY_FILE_MAX_SIZE + 1))
    }

    @Test
    fun `a password of the limit is accepted and one character more is not`() {
        assertFalse(CredentialLimits.isPasswordTooLong("a".repeat(CredentialLimits.PASSWORD_MAX_CHARS)))
        assertTrue(CredentialLimits.isPasswordTooLong("a".repeat(CredentialLimits.PASSWORD_MAX_CHARS + 1)))
    }

    @Test
    fun `a password of the minimum is accepted and one character less is not`() {
        assertEquals(8, CredentialLimits.PASSWORD_MIN_CHARS)
        assertFalse(CredentialLimits.isPasswordTooShort("a".repeat(CredentialLimits.PASSWORD_MIN_CHARS)))
        assertTrue(CredentialLimits.isPasswordTooShort("a".repeat(CredentialLimits.PASSWORD_MIN_CHARS - 1)))
    }

    @Test
    fun `the minimum is length alone - no rule on letters or digits`() {
        assertFalse(CredentialLimits.isPasswordTooShort("correct horse battery"))
        assertFalse(CredentialLimits.isPasswordTooShort("12345678"))
    }

    @Test
    fun `the minimum counts code points too - eight emoji are eight characters`() {
        assertFalse(CredentialLimits.isPasswordTooShort("😀".repeat(CredentialLimits.PASSWORD_MIN_CHARS)))
        assertTrue(CredentialLimits.isPasswordTooShort("😀".repeat(CredentialLimits.PASSWORD_MIN_CHARS - 1)))
    }

    // An emoji is two utf-16 units: counted by String.length, 256 of them would look like 512 characters
    // and be refused here while the core accepts them
    @Test
    fun `characters are counted as the core counts them, not in utf-16 units`() {
        val emoji = "😀"
        assertFalse(CredentialLimits.isPasswordTooLong(emoji.repeat(CredentialLimits.PASSWORD_MAX_CHARS)))
    }

    @Test
    fun `a key file never shows its content`() {
        val keyFile = KeyFile(name = "my.keyx", content = "secret".toByteArray())
        assertEquals("KeyFile(name=my.keyx, size=6)", keyFile.toString())
    }

    @Test
    fun `a wiped key file holds only zeros`() {
        val keyFile = KeyFile(name = "my.keyx", content = "secret".toByteArray())
        keyFile.wipe()
        assertTrue(keyFile.content.all { it == 0.toByte() })
    }
}
