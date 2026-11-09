package ru.kino.dev.storage

import kotlinx.coroutines.test.runTest
import org.junit.Test
import ru.kino.dev.core.DatabaseFileException
import kotlin.test.assertFailsWith

// The decision behind writeReplacing: a pipe cannot be shown in Robolectric, so its branch is tested here
// (pass-docs, Step 112)
class ReplacedContentTest {

    @Test
    fun `a file of the size written is written`() = runTest {
        ReplacedContent.verify(URI, BYTES, sizeAfterWrite = 16L) { error("A file of a known size is not read") }
    }

    @Test
    fun `a file of another size is not written`() = runTest {
        assertFailsWith<DatabaseFileException> {
            ReplacedContent.verify(URI, BYTES, sizeAfterWrite = 64L) { BYTES }
        }
    }

    @Test
    fun `a pipe that read back the bytes written is written`() = runTest {
        ReplacedContent.verify(URI, BYTES, sizeAfterWrite = null) { BYTES.copyOf() }
    }

    @Test
    fun `a pipe that read back something else is not written`() = runTest {
        assertFailsWith<DatabaseFileException> {
            ReplacedContent.verify(URI, BYTES, sizeAfterWrite = null) { BYTES + ByteArray(48) }
        }
    }

    private companion object {
        const val URI = "content://ru.kino.dev.test.documents/document/base.kdbx"
        val BYTES = ByteArray(16) { 2 }
    }
}
