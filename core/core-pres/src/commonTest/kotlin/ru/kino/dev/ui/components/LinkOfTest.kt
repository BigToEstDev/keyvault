package ru.kino.dev.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LinkOfTest {

    @Test
    fun `web addresses open as they are`() {
        assertEquals("https://mail.google.com", linkOf("https://mail.google.com"))
        assertEquals("http://192.168.0.1:8080/admin", linkOf("http://192.168.0.1:8080/admin"))
        assertEquals("https://Example.com", linkOf("HTTPS://Example.com"))
    }

    @Test
    fun `a bare host opens under https`() {
        assertEquals("https://mail.google.com", linkOf("mail.google.com"))
        assertEquals("https://example.com:8443/a?b=1#c", linkOf("example.com:8443/a?b=1#c"))
        assertEquals("https://почта.рф", linkOf("почта.рф"))
    }

    @Test
    fun `spaces around are dropped`() {
        assertEquals("https://example.com", linkOf("  https://example.com \n"))
    }

    @Test
    fun `other schemes open nothing`() {
        assertNull(linkOf("cmd://putty.exe -ssh host"))
        assertNull(linkOf("cmd://calc.exe"))
        assertNull(linkOf("file:///sdcard/a.txt"))
        assertNull(linkOf("intent://scan/#Intent;scheme=zxing;end"))
        assertNull(linkOf("javascript:alert(1)"))
        assertNull(linkOf("mailto:a@example.com"))
        assertNull(linkOf("ftp://example.com"))
    }

    @Test
    fun `placeholders, text and empty open nothing`() {
        assertNull(linkOf("{URL:RMVSCM}"))
        assertNull(linkOf("https://{USERNAME}.example.com"))
        assertNull(linkOf("my bank"))
        assertNull(linkOf("localhost"))
        assertNull(linkOf("https://"))
        assertNull(linkOf(""))
        assertNull(linkOf("   "))
    }
}
