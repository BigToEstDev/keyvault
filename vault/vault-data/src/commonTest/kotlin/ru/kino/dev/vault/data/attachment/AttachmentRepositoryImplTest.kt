package ru.kino.dev.vault.data.attachment

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlinx.coroutines.test.runTest
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.FakeNativeCore
import ru.kino.dev.core.expectFailure
import ru.kino.dev.vault.attachment.AttachmentFailure

class AttachmentRepositoryImplTest {

    private val core = FakeNativeCore()
    private val repository = AttachmentRepositoryImpl(core)

    @Test
    fun `the bytes of an attachment come from the core`() = runTest {
        core.attachmentBytes["h"] = byteArrayOf(1, 2, 3)

        assertContentEquals(byteArrayOf(1, 2, 3), repository.bytes(DB, "h"))
    }

    @Test
    fun `an attachment the database has not is not found`() = runTest {
        expectFailure<AttachmentFailure.NotFound> { repository.bytes(DB, "gone") }
    }

    @Test
    fun `any other refusal of the core is unexpected`() = runTest {
        core.failures["attachmentBytes"] = CoreException("DbKeyNotFound", "locked")

        expectFailure<AttachmentFailure.Unexpected> { repository.bytes(DB, "h") }
    }

    private companion object {
        const val DB = "content://db"
    }
}
