package ru.kino.dev.vault.attachment

import kotlin.test.Test
import kotlin.test.assertEquals

class AttachmentNamesTest {

    @Test
    fun `a name the entry has not stays as it is`() {
        assertEquals("photo.jpg", AttachmentNames.unique("photo.jpg", listOf("notes.txt")))
    }

    @Test
    fun `a name taken gets a number before the extension`() {
        assertEquals("photo (2).jpg", AttachmentNames.unique("photo.jpg", listOf("photo.jpg")))
    }

    @Test
    fun `the number goes past those taken`() {
        assertEquals("photo (4).jpg", AttachmentNames.unique("photo.jpg", listOf("photo.jpg", "photo (2).jpg", "photo (3).jpg")))
    }

    @Test
    fun `a name without an extension gets the number at its end`() {
        assertEquals("README (2)", AttachmentNames.unique("README", listOf("README")))
    }

    @Test
    fun `a name of its extension alone has none - the number goes at its end`() {
        assertEquals(".env (2)", AttachmentNames.unique(".env", listOf(".env")))
    }

    @Test
    fun `only the last extension is kept after the number`() {
        assertEquals("backup.tar (2).gz", AttachmentNames.unique("backup.tar.gz", listOf("backup.tar.gz")))
    }

    @Test
    fun `a blank name is unnamed, numbered as any other`() {
        assertEquals(AttachmentNames.UNNAMED, AttachmentNames.unique(" ", emptyList()))
        assertEquals("${AttachmentNames.UNNAMED} (2)", AttachmentNames.unique("", listOf(AttachmentNames.UNNAMED)))
    }
}
