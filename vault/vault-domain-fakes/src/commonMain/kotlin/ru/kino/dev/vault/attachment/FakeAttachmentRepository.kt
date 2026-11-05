package ru.kino.dev.vault.attachment

import ru.kino.dev.vault.entry.FakeEntryRepository

/**
 * [AttachmentRepository] over the bytes of [entries] ([FakeEntryRepository.attachmentBytes]): a copy each time, as
 * the core hands one. Every call is written to [calls].
 *
 * [failure] is thrown on every call, after it is recorded.
 */
class FakeAttachmentRepository(
    private val entries: FakeEntryRepository,
    val calls: MutableList<String> = mutableListOf(),
) : AttachmentRepository {

    var failure: Exception? = null

    /** The copies handed out, in order - a test checks they were wiped. */
    val handedOut = mutableListOf<ByteArray>()

    override suspend fun bytes(dbKey: String, id: String): ByteArray {
        calls += "bytes $id"
        failure?.let { throw it }
        val bytes = entries.attachmentBytes[id]?.copyOf() ?: throw AttachmentFailure.NotFound()
        return bytes.also { handedOut += it }
    }
}
