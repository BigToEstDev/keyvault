package ru.kino.dev.vault.pres.entryread

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.vault.entry.EntryContent
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryOpener
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupFailure
import ru.kino.dev.vault.group.GroupRepository
import ru.kino.dev.vault.pres.common.GroupChoice
import ru.kino.dev.vault.pres.common.groupChoice
import ru.kino.dev.vault.pres.navigation.EntryEditRoute

/**
 * @param loading the entry is not read yet - the skeleton is shown
 * @param content the entry; null until it is read
 * @param group the group the entry is in, with the path down to it; null until it is read
 * @param shownOwn indexes of protected own fields shown in clear
 * @param message for the snackbar, until it takes it
 */
data class EntryReadState(
    val loading: Boolean = true,
    val content: EntryContent? = null,
    val group: GroupChoice? = null,
    val passwordVisible: Boolean = false,
    val shownOwn: Set<Int> = emptySet(),
    val message: EntryReadMessage? = null,
)

/**
 * Reading an entry: its fields as the form lays them out, none of them taking input (pass-docs,
 * vault-our.md "Экран чтения записи", decisions.md "Экраны записи").
 *
 * The entry is read anew every time the screen is shown - it may come back from its form changed. What is
 * shown in clear stays shown while the entry is the same, and hides once it changed: the fields may have
 * moved.
 */
@HiltViewModel(assistedFactory = EntryReadProcessor.Factory::class)
class EntryReadProcessor @AssistedInject constructor(
    @Assisted("dbKey") private val dbKey: String,
    @Assisted("entryId") private val entryId: String,
    private val opener: EntryOpener,
    private val groupRepository: GroupRepository,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<EntryReadState, EntryReadState, Nothing> {

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("dbKey") dbKey: String, @Assisted("entryId") entryId: String): EntryReadProcessor
    }

    override val container = orbitContainer<EntryReadState, Nothing>(EntryReadState())

    fun action(action: EntryReadAction): Job = intent {
        when (action) {
            EntryReadAction.Load -> load()

            EntryReadAction.Back -> navigator.navigate(MviNavEvent.NavigateBack)

            EntryReadAction.Edit -> if (state.content != null) {
                navigator.navigate(MviNavEvent.NavigateTo(EntryEditRoute(dbKey, entryId)))
            }

            EntryReadAction.TogglePassword -> reduce { state.copy(passwordVisible = !state.passwordVisible) }

            is EntryReadAction.ToggleOwn -> reduce {
                val shown = state.shownOwn
                state.copy(shownOwn = if (action.index in shown) shown - action.index else shown + action.index)
            }

            EntryReadAction.MessageShown -> reduce { state.copy(message = null) }
        }
    }

    private suspend fun Syntax<EntryReadState, Nothing>.load() {
        val opened = try {
            opener.open(dbKey, EntryId(entryId))
        } catch (_: EntryFailure.EntryNotFound) {
            // Deleted or merged away while this screen was under another one
            navigator.navigate(MviNavEvent.NavigateBack)
            return
        } catch (failure: EntryFailure) {
            loadFailed(failure)
            return
        }
        // The group may be another one now - the entry comes back from its form moved
        val group = try {
            groupRepository.tree(dbKey).groupChoice(opened.groupId)
        } catch (failure: GroupFailure) {
            loadFailed(failure)
            return
        }
        val content = opened.content

        reduce {
            if (content == state.content) {
                state.copy(loading = false, group = group)
            } else {
                state.copy(loading = false, content = content, group = group, passwordVisible = false, shownOwn = emptySet())
            }
        }
    }

    private suspend fun Syntax<EntryReadState, Nothing>.loadFailed(failure: Exception) {
        log.unexpected(TAG, failure)
        reduce { state.copy(loading = false, message = EntryReadMessage.LoadFailed) }
    }

    private companion object {
        const val TAG = "EntryReadProcessor"
    }
}
