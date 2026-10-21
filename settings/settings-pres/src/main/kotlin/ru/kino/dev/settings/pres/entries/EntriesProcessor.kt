package ru.kino.dev.settings.pres.entries

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.core.DatabaseEntries
import ru.kino.dev.core.FieldProtection
import ru.kino.dev.core.HistoryLimits
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.settings.appearance.AppearanceRepository
import ru.kino.dev.vault.entries.EntrySettingsEditor
import ru.kino.dev.vault.entries.EntrySettingsFailure
import ru.kino.dev.vault.entries.EntrySettingsRepository
import ru.kino.dev.vault.entries.trims

/**
 * @param entries the settings of the entries; null until read - the screen is a skeleton meanwhile
 * @param keptItems the number of versions a limit switched on takes: the one it had on this screen, or the default
 * @param keptSize the same of the size, in bytes
 * @param choosing the wheel of a limit, while open
 * @param trimming "trim the history?", while asked
 * @param saving the part written into the file - every row waits, that one shows progress
 * @param message for the snackbar, until it takes it
 */
data class EntriesState(
    val entries: DatabaseEntries? = null,
    val keptItems: Int = HistoryChoices.DEFAULT_ITEMS,
    val keptSize: Long = HistoryChoices.DEFAULT_SIZE,
    val choosing: HistoryLimit? = null,
    val trimming: TrimDialog? = null,
    val saving: EntriesPart? = null,
    val message: EntriesMessage? = null,
)

/**
 * The entries of the database [dbKey] (pass-docs, Step 65): the fields hidden on every entry and how much history
 * an entry keeps. Kept in the file, as `Файл базы`: a change is made in the database and the file written at
 * once, with progress; not written - the change is made all the same and waits for the next save.
 *
 * A limit lower than it was, or one where there was none, drops versions already kept - the screen asks first.
 *
 * The user name made private takes it out of the list: "user name in the list" of the appearance goes off, for
 * every database - it is of the app (pass-docs, Step 56, item 5). Made open again, it stays off.
 */
@HiltViewModel(assistedFactory = EntriesProcessor.Factory::class)
class EntriesProcessor @AssistedInject constructor(
    @Assisted private val dbKey: String,
    private val settings: EntrySettingsRepository,
    private val editor: EntrySettingsEditor,
    private val appearance: AppearanceRepository,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<EntriesState, EntriesState, Nothing> {

    @AssistedFactory
    interface Factory {
        fun create(dbKey: String): EntriesProcessor
    }

    override val container = orbitContainer<EntriesState, Nothing>(EntriesState())

    fun action(action: EntriesAction): Job = intent {
        when (action) {
            EntriesAction.Load -> load()

            EntriesAction.Back -> navigator.navigate(MviNavEvent.NavigateBack)

            is EntriesAction.SetProtection -> setProtection(action.protection)

            is EntriesAction.LimitItems -> state.entries?.history?.let { history ->
                if (!action.limited) history.maxItems?.let { reduce { state.copy(keptItems = it) } }
                changeHistory(history.copy(maxItems = if (action.limited) state.keptItems else null), EntriesPart.Items)
            }

            is EntriesAction.LimitSize -> state.entries?.history?.let { history ->
                if (!action.limited) history.maxSize?.let { reduce { state.copy(keptSize = it) } }
                changeHistory(history.copy(maxSize = if (action.limited) state.keptSize else null), EntriesPart.Size)
            }

            is EntriesAction.Choose -> if (state.entries != null && state.saving == null) {
                reduce { state.copy(choosing = action.limit) }
            }

            is EntriesAction.SelectItems -> state.entries?.history?.let { history ->
                reduce { state.copy(choosing = null) }
                changeHistory(history.copy(maxItems = action.items), EntriesPart.Items)
            }

            is EntriesAction.SelectSize -> state.entries?.history?.let { history ->
                reduce { state.copy(choosing = null) }
                changeHistory(history.copy(maxSize = action.bytes), EntriesPart.Size)
            }

            EntriesAction.DismissChoice -> reduce { state.copy(choosing = null) }

            EntriesAction.TrimConfirm -> state.trimming?.let { dialog ->
                reduce { state.copy(trimming = null) }
                writeHistory(dialog.history, dialog.part)
            }

            EntriesAction.TrimDismiss -> reduce { state.copy(trimming = null) }

            EntriesAction.MessageShown -> reduce { state.copy(message = null) }
        }
    }

    private suspend fun Syntax<EntriesState, Nothing>.load() {
        try {
            val entries = settings.read(dbKey)
            reduce {
                state.copy(
                    entries = entries,
                    keptItems = entries.history.maxItems ?: state.keptItems,
                    keptSize = entries.history.maxSize ?: state.keptSize,
                )
            }
        } catch (failure: EntrySettingsFailure) {
            failed(failure)
        }
    }

    private suspend fun Syntax<EntriesState, Nothing>.setProtection(protection: FieldProtection) {
        val entries = state.entries ?: return
        if (state.saving != null || protection == entries.protection) return

        write(entries, entries.copy(protection = protection), EntriesPart.Protection) {
            editor.setProtection(dbKey, protection)
        }
        // Made - written or waiting for the next save; refused, the screen shows the protection as it was
        if (protection.userName && !entries.protection.userName && state.entries?.protection == protection) {
            hideUserNamesInList()
        }
    }

    private suspend fun Syntax<EntriesState, Nothing>.hideUserNamesInList() {
        try {
            if (appearance.userNameInList.first()) appearance.saveUserNameInList(false)
        } catch (failure: IOException) {
            // The list hides a private user name all the same: only the switch of the appearance stays on
            log.unexpected(TAG, failure)
        }
    }

    /** [history] kept - asked first when it drops versions already kept. */
    private suspend fun Syntax<EntriesState, Nothing>.changeHistory(history: HistoryLimits, part: EntriesPart) {
        val current = state.entries?.history ?: return
        if (state.saving != null || history == current) return

        if (history.trims(current)) {
            reduce { state.copy(trimming = TrimDialog(history, part)) }
        } else {
            writeHistory(history, part)
        }
    }

    private suspend fun Syntax<EntriesState, Nothing>.writeHistory(history: HistoryLimits, part: EntriesPart) {
        val entries = state.entries ?: return
        if (state.saving != null) return

        write(entries, entries.copy(history = history), part) { editor.setHistory(dbKey, history) }
    }

    /** Shows [changed] at once, while [change] writes it; refused - [before] again. */
    private suspend fun Syntax<EntriesState, Nothing>.write(
        before: DatabaseEntries,
        changed: DatabaseEntries,
        part: EntriesPart,
        change: suspend () -> Unit,
    ) {
        reduce { state.copy(entries = changed, saving = part) }
        try {
            change()
            reduce { state.copy(saving = null) }
        } catch (_: EntrySettingsFailure.NotSaved) {
            // The database has it: it is written with the next save
            reduce { state.copy(saving = null, message = EntriesMessage.NotSaved) }
        } catch (failure: EntrySettingsFailure) {
            reduce { state.copy(entries = before, saving = null) }
            failed(failure)
        }
    }

    private suspend fun Syntax<EntriesState, Nothing>.failed(failure: EntrySettingsFailure) {
        log.unexpected(TAG, failure)
        reduce { state.copy(message = EntriesMessage.Failed) }
    }

    private companion object {
        const val TAG = "EntriesProcessor"
    }
}
