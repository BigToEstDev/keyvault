package ru.kino.dev.vault.attachment

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import ru.kino.dev.core.FakeDatabaseFiles
import ru.kino.dev.core.expectFailure
import ru.kino.dev.vault.entry.FakeEntryRepository

class AttachmentFilesTest {

    private val files = FakeDatabaseFiles()
    private val entries = FakeEntryRepository()
    private val attachments = FakeAttachmentRepository(entries)
    private val attachmentFiles = AttachmentFiles(files, attachments)

    @Test
    fun `a file picked becomes a new attachment under the name its provider tells`() = runTest {
        files.contents[PICKED] = byteArrayOf(1, 2, 3)
        files.names[PICKED] = "photo.jpg"

        val added = attachmentFiles.read(PICKED, taken = emptyList())

        assertEquals("photo.jpg", added.name)
        assertContentEquals(byteArrayOf(1, 2, 3), added.bytes)
    }

    @Test
    fun `a name the form has already gets a number`() = runTest {
        files.contents[PICKED] = byteArrayOf(1)
        files.names[PICKED] = "photo.jpg"

        val added = attachmentFiles.read(PICKED, taken = listOf("photo.jpg"))

        assertEquals("photo (2).jpg", added.name)
    }

    @Test
    fun `a provider that tells no name gives the unnamed one`() = runTest {
        files.contents[PICKED] = byteArrayOf(1)

        assertEquals(AttachmentNames.UNNAMED, attachmentFiles.read(PICKED, taken = emptyList()).name)
    }

    @Test
    fun `a file of the limit exactly is added`() = runTest {
        files.contents[PICKED] = ByteArray(ATTACHMENT_MAX_BYTES)
        files.reportedSizes[PICKED] = ATTACHMENT_MAX_BYTES.toLong()

        assertEquals(ATTACHMENT_MAX_BYTES.toLong(), attachmentFiles.read(PICKED, taken = emptyList()).size)
    }

    @Test
    fun `a file larger by the size its provider tells is refused before a byte is read`() = runTest {
        files.contents[PICKED] = ByteArray(1)
        files.reportedSizes[PICKED] = ATTACHMENT_MAX_BYTES + 1L

        expectFailure<AttachmentFailure.TooLarge> { attachmentFiles.read(PICKED, taken = emptyList()) }

        assertEquals(listOf("size $PICKED"), files.journal)
    }

    @Test
    fun `a larger file of a provider that tells no size is refused by its bytes, read one past the limit`() = runTest {
        files.contents[PICKED] = ByteArray(ATTACHMENT_MAX_BYTES + 100)

        expectFailure<AttachmentFailure.TooLarge> { attachmentFiles.read(PICKED, taken = emptyList()) }

        assertEquals(listOf("size $PICKED", "readAtMost $PICKED"), files.journal)
    }

    @Test
    fun `a file that cannot be read fails as a file`() = runTest {
        files.unreachable += PICKED

        expectFailure<AttachmentFailure.FileFailed> { attachmentFiles.read(PICKED, taken = emptyList()) }
    }

    @Test
    fun `an attachment downloaded is written into the file picked, and its bytes wiped`() = runTest {
        entries.attachmentBytes[ID] = byteArrayOf(4, 5, 6)

        attachmentFiles.download(DB, ID, SAVED)

        assertContentEquals(byteArrayOf(4, 5, 6), files.contents.getValue(SAVED))
        assertContentEquals(ByteArray(3), attachments.handedOut.single())
    }

    @Test
    fun `a file that cannot be written fails as a file, the bytes wiped all the same`() = runTest {
        entries.attachmentBytes[ID] = byteArrayOf(4, 5, 6)
        files.unwritable += SAVED

        expectFailure<AttachmentFailure.FileFailed> { attachmentFiles.download(DB, ID, SAVED) }

        assertContentEquals(ByteArray(3), attachments.handedOut.single())
    }

    @Test
    fun `an attachment that is gone is not found, nothing is written`() = runTest {
        expectFailure<AttachmentFailure.NotFound> { attachmentFiles.download(DB, ID, SAVED) }

        assertTrue(files.journal.isEmpty())
    }

    private companion object {
        const val DB = "content://db"
        const val PICKED = "content://picked"
        const val SAVED = "content://saved"
        const val ID = "hash-1"
    }
}
