package ru.kino.dev.vault.data.attachment

import ru.kino.dev.core.CoreException
import ru.kino.dev.core.NativeCore
import ru.kino.dev.vault.attachment.AttachmentFailure
import ru.kino.dev.vault.attachment.AttachmentRepository
import kotlin.coroutines.cancellation.CancellationException

/**
 * [AttachmentRepository] over the native core. `NotFound` - the attachment is gone ([AttachmentFailure.NotFound]);
 * everything else (`DbKeyNotFound` after a lock, the bridge's own kinds, a bug) is [AttachmentFailure.Unexpected].
 */
internal class AttachmentRepositoryImpl(private val core: NativeCore) : AttachmentRepository {

    override suspend fun bytes(dbKey: String, id: String): ByteArray =
        try {
            core.attachmentBytes(dbKey, id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: CoreException) {
            throw if (e.kind == NOT_FOUND) AttachmentFailure.NotFound(e) else AttachmentFailure.Unexpected(e)
        } catch (e: Exception) {
            throw AttachmentFailure.Unexpected(e)
        }
}

private const val NOT_FOUND = "NotFound"
