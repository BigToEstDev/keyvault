package ru.kino.dev.vault.pres.trashentry

import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.core.FakeDatabaseFiles
import ru.kino.dev.core.phone.FakePhoneNumbers
import ru.kino.dev.core.preset.FakePresetRepository
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.vault.FakeVaultDatabase
import ru.kino.dev.vault.attachment.AttachmentFiles
import ru.kino.dev.vault.attachment.EntryAttachment
import ru.kino.dev.vault.attachment.FakeAttachmentRepository
import ru.kino.dev.vault.changes.FakeVaultChanges
import ru.kino.dev.vault.entries.FakeEntrySettingsRepository
import ru.kino.dev.vault.entry.EntryField
import ru.kino.dev.vault.entry.EntryOpener
import ru.kino.dev.vault.entry.EntrySummary
import ru.kino.dev.vault.entry.FakeEntryRepository
import ru.kino.dev.vault.group.FakeGroupRepository
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.pres.testing.DB
import ru.kino.dev.vault.pres.testing.RecordingLog
import ru.kino.dev.vault.pres.testing.recordNavigation
import ru.kino.dev.vault.pres.testing.runEach
import ru.kino.dev.vault.pres.trash.TrashDialog
import ru.kino.dev.vault.trash.FakeTrashRepository
import ru.kino.dev.vault.trash.TrashEditor
import ru.kino.dev.vault.trash.TrashEntry
import ru.kino.dev.vault.trash.TrashFailure

class TrashEntryProcessorTest {

    private val groups = FakeGroupRepository()
    private val work = groups.add("work", groups.rootId, "Work")
    private val entries = FakeEntryRepository()
    private val mail = entries.add(
        "mail",
        BIN,
        listOf(
            EntryField(StandardPreset.TITLE, "Mail", protected = false),
            EntryField(StandardPreset.PASSWORD, "secret", protected = true),
        ),
    )
    private val trash = FakeTrashRepository().apply {
        entries += TrashEntry(EntrySummary(mail, BIN, "Mail", "", createdAt = 0), previousGroupId = work, deletedAt = 500)
    }
    private val database = FakeVaultDatabase()
    private val log = RecordingLog()
    private val changes = FakeVaultChanges()
    private val files = FakeDatabaseFiles()

    @Test
    fun `an attachment of the deleted entry downloads into the file picked`() = runTest {
        val notes = EntryAttachment(id = "hash-notes", name = "notes.txt", size = 2)
        entries.attachmentBytes[notes.id] = byteArrayOf(7, 8)
        entries.stored[mail] = entries.stored.getValue(mail).copy(attachments = listOf(notes))

        val (state, _) = perform({ onLoad() }, { onDownload(0) }, { onDownloadPicked(SAVED) })

        assertEquals(listOf(7, 8), files.contents.getValue(SAVED).map { it.toInt() })
        assertEquals(TrashEntryMessage.AttachmentSaved, state.message)
    }

    @Test
    fun `the deleted entry is read with when it was deleted`() = runTest {
        val (state, _) = perform({ onLoad() })

        assertEquals("Mail", state.entry?.content?.value(StandardPreset.TITLE))
        assertEquals(500L, state.trashed?.deletedAt)
        assertFalse(state.loading)
    }

    @Test
    fun `an entry no longer in the bin sends the screen back`() = runTest {
        trash.entries.clear()

        val (state, events) = perform({ onLoad() })

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
        assertNull(state.entry)
    }

    @Test
    fun `a bin that cannot be read says so and goes to the log`() = runTest {
        trash.failures["entries"] = TrashFailure.Unexpected()

        val (state, _) = perform({ onLoad() })

        assertEquals(TrashEntryMessage.LoadFailed, state.message)
        assertEquals(1, log.logged.size)
    }

    @Test
    fun `restore offers the group the entry came from`() = runTest {
        val (state, _) = perform({ onLoad() }, { onRestore() })

        assertEquals(work, (state.dialog as TrashDialog.Restore).group)
    }

    @Test
    fun `restore offers the root when where the entry came from is not known`() = runTest {
        trash.entries[0] = trash.entries[0].copy(previousGroupId = null)

        val (state, _) = perform({ onLoad() }, { onRestore() })

        assertEquals(groups.rootId, (state.dialog as TrashDialog.Restore).group)
    }

    @Test
    fun `restored, the file is written and the screen goes back to the bin`() = runTest {
        val (_, events) = perform({ onLoad() }, { onRestore() }, { onDialogConfirm() })

        assertEquals(mapOf(mail to work), trash.restored)
        assertEquals(listOf("save"), database.calls)
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `removed for good after asking, back to the bin`() = runTest {
        val (state, events) = perform({ onLoad() }, { onRemove() }, { onDialogConfirm() })

        assertTrue(trash.entries.isEmpty())
        assertEquals(TrashDialog.Remove(1, saving = true), state.dialog)
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `cancelled, nothing changes`() = runTest {
        val (state, events) = perform({ onLoad() }, { onRemove() }, { onDialogDismiss() })

        assertNull(state.dialog)
        assertEquals(1, trash.entries.size)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `a file not written is said, and OK goes back`() = runTest {
        database.saveFailure = IOException("provider gone")

        val (state, events) = perform(
            { onLoad() },
            { onRemove() },
            { onDialogConfirm() },
            { onNotSavedSeen() },
        )

        assertTrue(state.notSaved)
        assertNull(state.dialog)
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `an entry gone from the bin meanwhile sends the screen back`() = runTest {
        trash.failures["remove"] = TrashFailure.Stale()

        val (_, events) = perform({ onLoad() }, { onRemove() }, { onDialogConfirm() })

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `the eye shows the password`() = runTest {
        val (state, _) = perform({ onLoad() }, { onTogglePassword() })

        assertTrue(state.passwordVisible)
    }

    private suspend fun TestScope.perform(vararg actions: TrashEntryActions.() -> Unit): Pair<TrashEntryState, List<MviNavEvent>> {
        val (navigator, events) = recordNavigation()
        val processor = TrashEntryProcessor(
            DB,
            mail.value,
            EntryOpener(entries, FakePresetRepository()),
            FakeEntrySettingsRepository(),
            trash,
            TrashEditor(trash, database),
            groups,
            FakePhoneNumbers(),
            changes,
            AttachmentFiles(files, FakeAttachmentRepository(entries)),
            navigator,
            log,
        )

        processor.testWithInternalState(this) {
            containerHost.runEach(actions)
            cancelAndIgnoreRemainingItems()
        }
        return processor.container.stateFlow.value to events
    }

    private companion object {
        val BIN = GroupId("bin")
        const val SAVED = "content://saved"
    }
}
