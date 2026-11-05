package ru.kino.dev.vault.pres.entryread

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import ru.kino.dev.core.CoreException
import ru.kino.dev.core.FakeDatabaseFiles
import ru.kino.dev.core.FieldProtection
import ru.kino.dev.core.phone.FakePhoneNumbers
import ru.kino.dev.core.preset.FakePresetRepository
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.settings.lock.FakeInactivityTimer
import ru.kino.dev.vault.FakeVaultDatabase
import ru.kino.dev.vault.attachment.AttachmentFiles
import ru.kino.dev.vault.attachment.EntryAttachment
import ru.kino.dev.vault.attachment.FakeAttachmentRepository
import ru.kino.dev.vault.changes.FakeVaultChanges
import ru.kino.dev.vault.entries.FakeEntrySettingsRepository
import ru.kino.dev.vault.entry.EntryEditor
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryField
import ru.kino.dev.vault.entry.EntryOpener
import ru.kino.dev.vault.entry.EntryTimes
import ru.kino.dev.vault.entry.FakeEntryRepository
import ru.kino.dev.vault.group.FakeGroupRepository
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.lock.AutoLock
import ru.kino.dev.vault.lock.DatabaseGuard
import ru.kino.dev.vault.pres.common.GroupChoice
import ru.kino.dev.vault.pres.navigation.EntryEditRoute
import ru.kino.dev.vault.pres.navigation.EntryHistoryRoute
import ru.kino.dev.vault.pres.testing.DB
import ru.kino.dev.vault.pres.testing.RecordingLog
import ru.kino.dev.vault.pres.testing.recordNavigation
import ru.kino.dev.vault.pres.testing.runEach
import ru.kino.dev.vault.trash.FakeTrashRepository

class EntryReadProcessorTest {

    private val entries = FakeEntryRepository()
    private val opener = EntryOpener(entries, FakePresetRepository())
    private val mail = entries.add(
        "mail",
        GroupId("root"),
        listOf(
            EntryField(StandardPreset.TITLE, "Mail", protected = false),
            EntryField(StandardPreset.PASSWORD, "secret", protected = true),
            EntryField("PIN", "1234", protected = true),
        ),
    )
    private val groups = FakeGroupRepository()
    private val database = FakeVaultDatabase()
    private val log = RecordingLog()
    private val settings = FakeEntrySettingsRepository()
    private val trash = FakeTrashRepository()
    private val changes = FakeVaultChanges()
    private val files = FakeDatabaseFiles()
    private val attachmentFiles = AttachmentFiles(files, FakeAttachmentRepository(entries))
    private val notes = EntryAttachment(id = "hash-notes", name = "notes.txt", size = 3)

    @Test
    fun `the attachments of the entry are read with it`() = runTest {
        withNotes()

        val (state, _) = perform({ onLoad() })

        assertEquals(listOf(notes), state.attachments)
    }

    @Test
    fun `download asks where to save the attachment`() = runTest {
        withNotes()

        val (state, _) = perform({ onLoad() }, { onDownload(0) })

        assertEquals(notes, state.saveAs)
    }

    @Test
    fun `the file picked gets the attachment, and the screen says it is saved`() = runTest {
        withNotes()

        val (state, _) = perform({ onLoad() }, { onDownload(0) }, { onDownloadPicked(SAVED) })

        assertEquals(listOf(1, 2, 3), files.contents.getValue(SAVED).map { it.toInt() })
        assertEquals(EntryReadMessage.AttachmentSaved, state.message)
        assertNull(state.saveAs)
        assertNull(state.downloading)
    }

    @Test
    fun `a file that cannot be written says so, nothing else changes`() = runTest {
        withNotes()
        files.unwritable += SAVED

        val (state, _) = perform({ onLoad() }, { onDownload(0) }, { onDownloadPicked(SAVED) })

        assertEquals(EntryReadMessage.AttachmentNotSaved, state.message)
        assertTrue(log.logged.isEmpty())
    }

    @Test
    fun `a file picked with no download asked writes nothing`() = runTest {
        withNotes()

        perform({ onLoad() }, { onDownloadPicked(SAVED) })

        assertTrue(files.journal.isEmpty())
    }

    /** The entry gets the attachment [notes], its bytes 1, 2, 3. */
    private fun withNotes() {
        entries.attachmentBytes[notes.id] = byteArrayOf(1, 2, 3)
        entries.stored[mail] = entries.stored.getValue(mail).copy(attachments = listOf(notes))
    }

    @Test
    fun `the group of the entry is shown with the path down to it`() = runTest {
        val (state, _) = perform({ onLoad() })

        assertEquals(GroupChoice(groups.rootId, listOf("Passwords")), state.group)
    }

    @Test
    fun `an entry moved on its form comes back in its new group`() = runTest {
        val shown = EntryReadState(
            loading = false,
            content = opener.open(DB, mail).content,
            group = GroupChoice(groups.rootId, listOf("Passwords")),
        )
        val work = groups.add("work", groups.rootId, "Work")
        entries.stored[mail] = entries.stored.getValue(mail).copy(groupId = work)

        val (state, _) = perform({ onLoad() }, initial = shown)

        assertEquals(GroupChoice(work, listOf("Passwords", "Work")), state.group)
    }

    @Test
    fun `the entry is read when the screen is shown, laid over the preset, everything hidden`() = runTest {
        val (state, _) = perform({ onLoad() })

        assertFalse(state.loading)
        assertEquals("Mail", state.content?.value(StandardPreset.TITLE))
        assertEquals(listOf("PIN"), state.content?.ownFields?.map { it.key })
        assertFalse(state.passwordVisible)
        assertTrue(state.shownOwn.isEmpty())
    }

    @Test
    fun `the fields private by the database come with the entry, and the eye shows one`() = runTest {
        settings.entries = settings.entries.copy(protection = FieldProtection.DEFAULT.copy(title = true, url = true))

        val (state, _) = perform({ onLoad() }, { onToggleKey(StandardPreset.TITLE) })

        assertEquals(FieldProtection.DEFAULT.copy(title = true, url = true), state.protection)
        assertEquals(setOf(StandardPreset.TITLE), state.shownKeys)
    }

    @Test
    fun `the private fields changed in the settings - the entry comes back hidden again`() = runTest {
        val shown = EntryReadState(
            loading = false,
            protection = FieldProtection.DEFAULT,
            shownKeys = setOf(StandardPreset.TITLE),
        )
        settings.entries = settings.entries.copy(protection = FieldProtection.DEFAULT.copy(title = true, url = true))

        val (state, _) = perform({ onLoad() }, initial = shown)

        assertEquals(FieldProtection.DEFAULT.copy(title = true, url = true), state.protection)
        assertTrue(state.shownKeys.isEmpty())
    }

    @Test
    fun `what is shown stays shown when the entry comes back the same`() = runTest {
        val (state, _) = perform(
            { onLoad() },
            { onTogglePassword() },
            { onToggleOwn(0) },
            { onLoad() },
        )

        assertTrue(state.passwordVisible)
        assertEquals(setOf(0), state.shownOwn)
    }

    @Test
    fun `an entry changed on its form comes back read anew and hidden again`() = runTest {
        val shown = EntryReadState(
            loading = false,
            content = opener.open(DB, mail).content,
            passwordVisible = true,
            shownOwn = setOf(0),
        )
        entries.stored[mail] = entries.stored.getValue(mail).let { entry ->
            entry.copy(fields = entry.fields.map { if (it.key == StandardPreset.TITLE) it.copy(value = "Post") else it })
        }

        val (state, _) = perform({ onLoad() }, initial = shown)

        assertEquals("Post", state.content?.value(StandardPreset.TITLE))
        assertFalse(state.passwordVisible)
        assertTrue(state.shownOwn.isEmpty())
    }

    @Test
    fun `an entry gone closes the screen`() = runTest {
        entries.stored.remove(mail)

        val (_, events) = perform({ onLoad() })

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `an entry that cannot be read says so and goes to the log`() = runTest {
        entries.failures["read"] = EntryFailure.Unexpected(IllegalStateException("broken"))

        val (state, _) = perform({ onLoad() }, { onEdit() })

        assertFalse(state.loading)
        assertNull(state.content)
        assertEquals(EntryReadMessage.LoadFailed, state.message)
        assertEquals(1, log.logged.size)
    }

    @Test
    fun `the pen opens the form of this entry, back goes back`() = runTest {
        val (_, events) = perform({ onLoad() }, { onEdit() }, { onBack() })

        assertEquals(
            listOf(MviNavEvent.NavigateTo(EntryEditRoute(DB, mail.value)), MviNavEvent.NavigateBack),
            events,
        )
    }

    @Test
    fun `the entry is read with when it was made and last changed`() = runTest {
        entries.stored[mail] = entries.stored.getValue(mail).copy(createdAt = 1_000L, modifiedAt = 2_000L)

        val (state, _) = perform({ onLoad() })

        assertEquals(EntryTimes(created = 1_000L, modified = 2_000L), state.times)
    }

    @Test
    fun `back from its form, the entry shows the new time of its change`() = runTest {
        val read = opener.open(DB, mail)
        val shown = EntryReadState(
            loading = false,
            content = read.content,
            group = GroupChoice(groups.rootId, listOf("Passwords")),
            times = read.times,
        )
        entries.stored[mail] = entries.stored.getValue(mail).copy(modifiedAt = 9_000L)

        val (state, _) = perform({ onLoad() }, initial = shown)

        assertEquals(9_000L, state.times.modified)
    }

    @Test
    fun `history in the menu opens the history of this entry`() = runTest {
        val (_, events) = perform({ onLoad() }, { onHistory() })

        assertEquals(listOf(MviNavEvent.NavigateTo(EntryHistoryRoute(DB, mail.value))), events)
    }

    @Test
    fun `no history before the entry is read`() = runTest {
        val (_, events) = perform({ onHistory() })

        assertTrue(events.isEmpty())
    }

    // Delete, from the menu of the top bar

    @Test
    fun `delete asks first, naming the entry`() = runTest {
        val (state, _) = perform({ onLoad() }, { onDelete() })

        assertEquals(DeleteDialog("Mail", toBin = true), state.delete)
    }

    @Test
    fun `without a recycle bin the dialog says the delete is for good`() = runTest {
        trash.enabled = false

        val (state, _) = perform({ onLoad() }, { onDelete() })

        assertEquals(DeleteDialog("Mail", toBin = false), state.delete)
    }

    @Test
    fun `nothing to delete before the entry is read`() = runTest {
        val (state, _) = perform({ onDelete() })

        assertNull(state.delete)
    }

    @Test
    fun `a deleted entry goes to the recycle bin, the file is written and the screen goes back`() = runTest {
        val (_, events) = perform({ onLoad() }, { onDelete() }, { onDeleteConfirm() })

        assertEquals(listOf(mail), entries.recycled)
        assertEquals(listOf("save"), database.calls)
        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `a deleted entry that is not written stays here, and the snackbar says so`() = runTest {
        database.saveFailure = CoreException("Io", "disk full")

        val (state, events) = perform({ onLoad() }, { onDelete() }, { onDeleteConfirm() })

        assertNull(state.delete)
        assertEquals(EntryReadMessage.NotSaved, state.message)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `cancel closes the dialog, the entry stays`() = runTest {
        val (state, _) = perform({ onLoad() }, { onDelete() }, { onDeleteDismiss() })

        assertNull(state.delete)
        assertTrue(entries.recycled.isEmpty())
    }

    @Test
    fun `an entry in the recycle bin is not shown - the screen goes back`() = runTest {
        // Deleted from its form over this screen: back here, it is in the bin - in a group put there
        val old = groups.add("old", groups.rootId, "Old")
        groups.delete(DB, old)
        entries.stored[mail] = entries.stored.getValue(mail).copy(groupId = old)

        val (_, events) = perform({ onLoad() })

        assertEquals(listOf(MviNavEvent.NavigateBack), events)
    }

    @Test
    fun `the lock button locks the database for real and leaves the locked screen to the app`() = runTest {
        val lock = lock()
        lock.watch(DB)

        val (state, events) = perform({ onLock() }, autoLock = lock)

        assertEquals(listOf("lock"), database.calls)
        assertEquals(DB, lock.locked.first())
        assertTrue(state.locking)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `a lock the core refuses says so and lets the button be tapped again`() = runTest {
        database.lockFailure = CoreException("DbKeyNotFound", "not open")
        val lock = lock()
        lock.watch(DB)

        val (state, _) = perform({ onLock() }, autoLock = lock)

        assertFalse(state.locking)
        assertEquals(EntryReadMessage.Unexpected, state.message)
    }

    /** The lock of the app, the timer never running out here - the tests lock by hand. */
    private fun TestScope.lock(): AutoLock = AutoLock(
        database = database,
        guard = DatabaseGuard(),
        timer = FakeInactivityTimer(),
        scope = backgroundScope,
        onFailure = { log.unexpected("AutoLock", it) },
    )

    @Test
    fun `a change made elsewhere while the entry is shown is read again`() = runTest {
        val (navigator, _) = recordNavigation()
        val editor = EntryEditor(entries, opener, settings, database)
        val processor =
            EntryReadProcessor(DB, mail.value, opener, settings, groups, editor, trash, lock(), FakePhoneNumbers(), changes, attachmentFiles, navigator, log)

        processor.testWithInternalState(this) {
            // Listening, as while the screen is shown
            runOnCreate()
            val stored = entries.stored.getValue(mail)
            entries.stored[mail] = stored.copy(
                fields = stored.fields.map { if (it.key == StandardPreset.TITLE) it.copy(value = "Mail, renamed") else it },
            )
            changes.changed(DB)

            val title = awaitInternalState().content?.presetFields?.first { it.key == StandardPreset.TITLE }?.value
            assertEquals("Mail, renamed", title)
            cancelAndIgnoreRemainingItems()
        }
    }

    /** Runs [actions] one after another on a new processor; the state they end in, and the navigation. */
    private suspend fun TestScope.perform(
        vararg actions: EntryReadActions.() -> Unit,
        initial: EntryReadState = EntryReadState(),
        autoLock: AutoLock = lock(),
    ): Pair<EntryReadState, List<MviNavEvent>> {
        val (navigator, events) = recordNavigation()
        val editor = EntryEditor(entries, opener, settings, database)
        val processor = EntryReadProcessor(
            DB, mail.value, opener, settings, groups, editor, trash, autoLock, FakePhoneNumbers(), changes, attachmentFiles, navigator, log,
        )

        processor.testWithInternalState(this, initial) {
            containerHost.runEach(actions)
            cancelAndIgnoreRemainingItems()
        }
        return processor.container.stateFlow.value to events
    }

    private companion object {
        const val SAVED = "content://saved"
    }
}
