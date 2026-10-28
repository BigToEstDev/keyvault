package ru.kino.dev.vault.pres.entryedit

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import ru.kino.dev.core.FieldProtection
import ru.kino.dev.core.FieldType
import ru.kino.dev.core.phone.PhoneDefaults
import ru.kino.dev.core.phone.PhoneNumbers
import ru.kino.dev.core.phone.PhoneType
import ru.kino.dev.core.preset.EntryPreset
import ru.kino.dev.core.preset.PresetId
import ru.kino.dev.core.preset.PresetRepository
import ru.kino.dev.core.preset.PresetValidator
import ru.kino.dev.core.preset.StandardPreset
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.mvi.textIntent
import ru.kino.dev.navigation.MviNavEvent
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.navigation.PresetEditRoute
import ru.kino.dev.vault.entries.EntrySettingsRepository
import ru.kino.dev.vault.entry.EntryContent
import ru.kino.dev.vault.entry.EntryDraft
import ru.kino.dev.vault.entry.EntryEditor
import ru.kino.dev.vault.entry.EntryFailure
import ru.kino.dev.vault.entry.EntryField
import ru.kino.dev.vault.entry.EntryOpener
import ru.kino.dev.vault.entry.EntryValidator
import ru.kino.dev.vault.entry.FieldProblem
import ru.kino.dev.vault.entry.NameProblem
import ru.kino.dev.vault.entry.blank
import ru.kino.dev.vault.group.EntryId
import ru.kino.dev.vault.group.GroupFailure
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.group.GroupRepository
import ru.kino.dev.vault.pres.common.GroupChoice
import ru.kino.dev.vault.pres.common.groupChoices
import ru.kino.dev.vault.pres.common.protectionOrDefault
import ru.kino.dev.vault.pres.common.toggled
import ru.kino.dev.vault.pres.navigation.EntryReadRoute
import ru.kino.dev.vault.trash.TrashRepository
import ru.kino.dev.vault.trash.deletesToBin

/**
 * @param creating a new entry, not one of the database
 * @param loading the entry is being read - the skeleton is shown; never for a new one
 * @param content what the form holds now; null until the entry is read
 * @param initial what the form opened with - the blank or the entry as read: "back" asks before leaving
 * anything else
 * @param groups every group the entry can go into, in the order of the tree; empty until the tree is read
 * @param group the group chosen - where a new entry goes, where an edited one is moved on saving
 * @param initialGroup the group the form opened with: another one is a change, as other fields are
 * @param hidden filled fields of a preset chosen before that the preset now chosen lacks (see [EntryDraft]);
 * memory only, as the whole state - never the saved state of the screen
 * @param presetsBeforeCreate the presets of one's own when "+ create preset" was tapped: one not among them,
 * once the screen is back, is the one made there - it is picked (pass-docs, Step 49); null otherwise
 * @param shownOwn indexes of protected own fields shown in clear
 * @param protection the fields private by the database (pass-docs, Step 65); only the password until read
 * @param shownKeys keys of the fields of the preset private by the database shown in clear
 * @param phoneRegion the country a phone field of none yet is shown with - a field of a preset; null until read
 *   (pass-docs, Step 77)
 * @param problems what the last save was refused for, until the field is edited
 * @param saving the change is being made and written - the fab waits, the fields take nothing
 * @param message for the snackbar, until it takes it
 */
data class EntryEditState(
    val creating: Boolean,
    val loading: Boolean = false,
    val content: EntryContent? = null,
    val initial: EntryContent? = null,
    val groups: List<GroupChoice> = emptyList(),
    val group: GroupId? = null,
    val initialGroup: GroupId? = null,
    val hidden: List<EntryField> = emptyList(),
    val presetsBeforeCreate: Set<PresetId>? = null,
    val passwordVisible: Boolean = false,
    val shownOwn: Set<Int> = emptySet(),
    val protection: FieldProtection = FieldProtection.DEFAULT,
    val shownKeys: Set<String> = emptySet(),
    val phoneRegion: String? = null,
    val problems: List<FieldProblem> = emptyList(),
    val saving: Boolean = false,
    val dialog: EntryEditDialog? = null,
    val message: EntryEditMessage? = null,
) {
    /** The form differs from what it opened with - the fields, or the group. */
    val changed: Boolean get() = content != initial || group != initialGroup

    /** One more own field can be added - the card of own fields has room (pass-docs, Step 40, fix 19.1). */
    val canAddField: Boolean get() = content?.let(EntryValidator::canAddField) == true

    /** The required field of the preset under [key] was refused for being empty. */
    fun isMissing(key: String): Boolean = problems.any { it is FieldProblem.Required && it.key == key }

    /** Why the name of the own field at [index] was refused, if it was. */
    fun nameProblem(index: Int): NameProblem? =
        problems.firstNotNullOfOrNull { (it as? FieldProblem.BadName)?.takeIf { bad -> bad.index == index }?.problem }
}

/**
 * Creating an entry, or editing one - one form, two ways in (pass-docs, vault-our.md "Экран редактирования
 * записи", decisions.md "Экраны записи").
 *
 * The checks are [EntryValidator]'s, the same the save is refused by: the form does not check twice, it
 * shows what [EntryEditor] refuses. A save that goes through, or a database that cannot take it any more -
 * the entry or its group gone - closes the screen; the screen it goes back to reads anew.
 *
 * The entry is read once, when the screen opens: what the person types must not be lost when the screen is
 * shown again.
 *
 * @param groupId where a new entry goes; null when editing
 * @param entryId the entry edited; null when creating
 */
class EntryEditProcessor(
    private val dbKey: String,
    private val groupId: String?,
    private val entryId: String?,
    private val opener: EntryOpener,
    private val settings: EntrySettingsRepository,
    private val groupRepository: GroupRepository,
    private val editor: EntryEditor,
    private val trash: TrashRepository,
    private val presets: PresetRepository,
    private val phones: PhoneNumbers,
    private val phoneDefaults: PhoneDefaults,
    private val navigator: Navigator,
    private val log: ErrorLog,
) : ViewModel(), OrbitContainerHost<EntryEditState, EntryEditState, Nothing> {

    init {
        require((groupId == null) != (entryId == null)) { "either a group to create in or an entry to edit" }
    }

    override val container = orbitContainer<EntryEditState, Nothing>(
        if (entryId == null) {
            val blank = StandardPreset.preset.blank()
            val group = GroupId(checkNotNull(groupId))
            EntryEditState(creating = true, content = blank, initial = blank, group = group, initialGroup = group)
        } else {
            EntryEditState(creating = false, loading = true)
        },
    ) {
        if (entryId != null) load(EntryId(entryId)) else loadGroups()
    }

    fun action(action: EntryEditAction): Job = when (action) {
        // Typed text: the state is updated before the field draws again, or the cursor jumps
        is EntryEditAction.ValueChanged -> textIntent {
            reduce {
                state.edited { withValue(action.key, action.value) }
                    .copy(problems = state.problems.filterNot { it is FieldProblem.Required && it.key == action.key })
            }
        }

        is EntryEditAction.OwnValueChanged -> textIntent {
            reduce {
                state.edited {
                    copy(ownFields = ownFields.mapIndexed { i, field -> if (i == action.index) field.copy(value = action.value) else field })
                }
            }
        }

        is EntryEditAction.DialogNameChanged -> textIntent {
            reduce { state.copy(dialog = state.dialog?.typed(action.name)) }
        }

        is EntryEditAction.PhoneDigitsChanged -> textIntent {
            reduce { state.withPhoneDigits(action.target, action.digits) }
        }

        else -> intent { handle(action) }
    }

    private suspend fun Syntax<EntryEditState, Nothing>.handle(action: EntryEditAction) {
        when (action) {
            is EntryEditAction.ValueChanged,
            is EntryEditAction.OwnValueChanged,
            is EntryEditAction.DialogNameChanged,
            is EntryEditAction.PhoneDigitsChanged,
            -> Unit // in action()

            is EntryEditAction.ChoosePhoneCountry -> state.phoneField(action.target)?.let { field ->
                val selected = state.regionOf(field) ?: return@let
                if (state.editable) {
                    val countries = phones.countries()
                    reduce { state.copy(dialog = EntryEditDialog.PhoneCountries(action.target, selected, countries)) }
                }
            }

            is EntryEditAction.PhoneCountryChosen -> (state.dialog as? EntryEditDialog.PhoneCountries)?.let { dialog ->
                phones.prepare(listOf(action.region))
                reduce { state.withPhoneCountry(dialog.target, action.region).copy(dialog = null) }
            }

            EntryEditAction.TogglePassword -> reduce { state.copy(passwordVisible = !state.passwordVisible) }

            is EntryEditAction.ToggleOwn -> reduce { state.copy(shownOwn = state.shownOwn.toggled(action.index)) }

            is EntryEditAction.ToggleKey -> reduce { state.copy(shownKeys = state.shownKeys.toggled(action.key)) }

            is EntryEditAction.GroupChosen -> if (state.editable) {
                reduce { state.copy(group = action.id) }
            }

            EntryEditAction.ChoosePreset -> state.content?.takeIf { state.editable }?.let { content ->
                val own = presets.presets.first()
                reduce { state.copy(dialog = EntryEditDialog.Preset(own, content.preset.id)) }
            }

            is EntryEditAction.PresetChosen -> {
                val preset = (state.dialog as? EntryEditDialog.Preset)?.presets?.firstOrNull { it.id == action.id }
                    ?: StandardPreset.preset.takeIf { action.id == null }
                preset?.let { switchPreset(it) }
            }

            EntryEditAction.CreatePreset -> (state.dialog as? EntryEditDialog.Preset)?.let { dialog ->
                if (PresetValidator.canAdd(dialog.presets)) {
                    reduce { state.copy(dialog = null, presetsBeforeCreate = dialog.presets.mapNotNull { it.id }.toSet()) }
                    navigator.navigate(MviNavEvent.NavigateTo(PresetEditRoute()))
                }
            }

            EntryEditAction.Resumed -> state.presetsBeforeCreate?.let { before ->
                reduce { state.copy(presetsBeforeCreate = null) }
                // None new - the person went back without making one
                presets.presets.first().firstOrNull { it.id !in before }?.let { switchPreset(it) }
            }

            EntryEditAction.AddField -> if (state.editable && state.canAddField) {
                reduce { state.copy(dialog = EntryEditDialog.AddField()) }
            }

            is EntryEditAction.EditField -> state.ownField(action.index)?.let { field ->
                if (state.editable) {
                    reduce {
                        state.copy(dialog = EntryEditDialog.EditField(action.index, field.key, field.protected, field.type))
                    }
                }
            }

            is EntryEditAction.DialogProtectedChanged -> reduce {
                state.copy(dialog = state.dialog?.switched(action.protected))
            }

            is EntryEditAction.DialogTypeChanged -> reduce {
                state.copy(dialog = state.dialog?.typedAs(action.type))
            }

            is EntryEditAction.DeleteField -> state.ownField(action.index)?.let { field ->
                if (state.editable) reduce { state.copy(dialog = EntryEditDialog.DeleteField(action.index, field.key)) }
            }

            EntryEditAction.DialogConfirm -> confirm()

            EntryEditAction.DialogDismiss -> when (val dialog = state.dialog) {
                // Nothing to stay for: the change is made, the screen goes as after a save
                EntryEditDialog.NotSaved -> leave()
                is EntryEditDialog.Delete -> if (!dialog.deleting) reduce { state.copy(dialog = null) }
                else -> reduce { state.copy(dialog = null) }
            }

            EntryEditAction.Save -> save(dropHidden = false)

            EntryEditAction.KeepHidden -> if (state.dialog is EntryEditDialog.HiddenFields) {
                state.content?.let { content ->
                    val kept = EntryDraft(content, state.hidden).withHiddenAsOwn()
                    reduce { state.copy(content = kept.content, hidden = kept.hidden, dialog = null) }
                    save(dropHidden = false)
                }
            }

            EntryEditAction.Back -> when {
                state.saving -> Unit
                state.content != null && state.changed -> reduce { state.copy(dialog = EntryEditDialog.Leave) }
                else -> leave()
            }

            EntryEditAction.Delete -> state.initial?.let { read ->
                // A new entry has nothing to delete; nor is a dialog put over another one
                if (entryId != null && state.editable && state.dialog == null) {
                    val toBin = trash.deletesToBin(dbKey)
                    reduce { state.copy(dialog = EntryEditDialog.Delete(read.value(StandardPreset.TITLE), toBin)) }
                }
            }

            EntryEditAction.MessageShown -> reduce { state.copy(message = null) }
        }
    }

    /** Into the recycle bin and the file; done, the screen goes back - what was typed goes with the entry. */
    private suspend fun Syntax<EntryEditState, Nothing>.delete(dialog: EntryEditDialog.Delete) {
        val id = entryId ?: return
        if (dialog.deleting) return

        reduce { state.copy(dialog = dialog.copy(deleting = true)) }
        try {
            editor.delete(dbKey, EntryId(id))
            leaveDeleted(id)
        } catch (failure: EntryFailure) {
            when (failure) {
                // In the recycle bin, but not in the file: the error is shown here (pass-docs, Step 50, item 2)
                is EntryFailure.NotSaved -> reduce { state.copy(dialog = null, message = EntryEditMessage.NotSaved) }

                // Gone already - deleted or merged away
                is EntryFailure.EntryNotFound -> leaveDeleted(id)

                // Not what deleting an entry throws
                is EntryFailure.InvalidFields, is EntryFailure.GroupNotFound, is EntryFailure.Unexpected -> {
                    log.unexpected(TAG, failure)
                    reduce { state.copy(dialog = null, message = EntryEditMessage.Unexpected) }
                }
            }
        }
    }

    private suspend fun Syntax<EntryEditState, Nothing>.load(id: EntryId) {
        try {
            val opened = opener.open(dbKey, id)
            val groups = groupRepository.tree(dbKey).groupChoices()
            val protection = settings.protectionOrDefault(dbKey, log, TAG)
            val phoneRegion = preparePhones(opened.content)
            reduce {
                state.copy(
                    loading = false,
                    protection = protection,
                    phoneRegion = phoneRegion,
                    content = opened.content,
                    initial = opened.content,
                    groups = groups,
                    group = opened.groupId,
                    initialGroup = opened.groupId,
                )
            }
        } catch (_: EntryFailure.EntryNotFound) {
            leave()
        } catch (failure: EntryFailure) {
            loadFailed(failure)
        } catch (failure: GroupFailure) {
            loadFailed(failure)
        }
    }

    /** A new entry: the groups to choose from - the one it goes into is known from the route already. */
    private suspend fun Syntax<EntryEditState, Nothing>.loadGroups() {
        try {
            val groups = groupRepository.tree(dbKey).groupChoices()
            // Hidden while typed as it will be in the file (pass-docs, Step 65)
            val protection = settings.protectionOrDefault(dbKey, log, TAG)
            val phoneRegion = state.content?.let { preparePhones(it) }
            reduce { state.copy(groups = groups, protection = protection, phoneRegion = phoneRegion) }
        } catch (failure: GroupFailure) {
            loadFailed(failure)
        }
    }

    private suspend fun Syntax<EntryEditState, Nothing>.loadFailed(failure: Exception) {
        log.unexpected(TAG, failure)
        reduce { state.copy(loading = false, message = EntryEditMessage.LoadFailed) }
    }

    private suspend fun Syntax<EntryEditState, Nothing>.confirm() {
        val content = state.content ?: return
        when (val dialog = state.dialog) {
            null -> Unit

            is EntryEditDialog.AddField -> {
                val problem = EntryValidator.nameProblem(content, dialog.name)
                if (problem != null) {
                    reduce { state.copy(dialog = dialog.copy(problem = problem)) }
                } else {
                    val field = EntryField(
                        EntryValidator.normalizeName(dialog.name),
                        value = "",
                        protected = dialog.protected,
                        type = dialog.type.withRegion(state.phoneRegion),
                    )
                    reduce { state.edited { copy(ownFields = ownFields + field) }.copy(dialog = null) }
                }
            }

            is EntryEditDialog.EditField -> {
                val problem = EntryValidator.nameProblem(content, dialog.name, renamedIndex = dialog.index)
                if (problem != null) {
                    reduce { state.copy(dialog = dialog.copy(problem = problem)) }
                } else {
                    val name = EntryValidator.normalizeName(dialog.name)
                    reduce {
                        state.edited {
                            copy(
                                ownFields = ownFields.mapIndexed { i, field ->
                                    if (i == dialog.index) {
                                        field.edited(name, dialog.protected, dialog.type.withRegion(state.phoneRegion))
                                    } else {
                                        field
                                    }
                                },
                            )
                        }.copy(
                            dialog = null,
                            problems = state.problems.withoutName(dialog.index),
                            // Protected now - it starts hidden, as one read so does
                            shownOwn = if (dialog.protected) state.shownOwn - dialog.index else state.shownOwn,
                        )
                    }
                }
            }

            is EntryEditDialog.DeleteField -> reduce {
                state.edited { copy(ownFields = ownFields.filterIndexed { i, _ -> i != dialog.index }) }
                    .withoutOwnField(dialog.index)
                    .copy(dialog = null)
            }

            is EntryEditDialog.Preset, is EntryEditDialog.PhoneCountries -> reduce { state.copy(dialog = null) }

            is EntryEditDialog.HiddenFields -> {
                reduce { state.copy(dialog = null) }
                save(dropHidden = true)
            }

            EntryEditDialog.Leave, EntryEditDialog.NotSaved -> leave()

            is EntryEditDialog.Delete -> delete(dialog)
        }
    }

    /**
     * The form laid over [preset]: the values go by key, what the new preset lacks is hidden (see
     * [EntryDraft]). What was said about the fields - shown in clear, refused - goes: they may have moved.
     */
    private suspend fun Syntax<EntryEditState, Nothing>.switchPreset(preset: EntryPreset) {
        val content = state.content ?: return
        if (!state.editable) return
        val draft = EntryDraft(content, state.hidden).withPreset(preset)
        reduce {
            state.copy(
                content = draft.content,
                hidden = draft.hidden,
                dialog = null,
                shownOwn = emptySet(),
                problems = emptyList(),
            )
        }
    }

    /**
     * Checks, makes the change and writes. Fields hidden by the preset chosen are asked about first, unless
     * [dropHidden] - the person said "save as is"; they stay in the form until the save goes through.
     */
    private suspend fun Syntax<EntryEditState, Nothing>.save(dropHidden: Boolean) {
        val content = state.content ?: return
        val group = state.group ?: return
        if (state.saving) return
        if (!dropHidden && EntryDraft(content, state.hidden).losesHidden) {
            reduce { state.copy(dialog = EntryEditDialog.HiddenFields(state.hidden.map { it.key })) }
            return
        }

        reduce { state.copy(saving = true, problems = emptyList()) }
        try {
            if (entryId == null) {
                editor.create(dbKey, group, content)
            } else {
                editor.update(dbKey, EntryId(entryId), content, group)
            }
            leave()
        } catch (failure: EntryFailure) {
            failed(failure)
        }
    }

    private suspend fun Syntax<EntryEditState, Nothing>.failed(failure: EntryFailure) {
        when (failure) {
            is EntryFailure.InvalidFields -> reduce { state.copy(saving = false, problems = failure.problems) }

            // Made, but not in the file. Staying would not help: a second save of a new entry would make a
            // second entry - the first one's id is not known here (pass-docs, Step 38, decision 13)
            is EntryFailure.NotSaved -> reduce { state.copy(saving = false, dialog = EntryEditDialog.NotSaved) }

            // Nothing to save into any more; the screen behind reads what is there
            is EntryFailure.EntryNotFound, is EntryFailure.GroupNotFound -> leave()

            is EntryFailure.Unexpected -> {
                log.unexpected(TAG, failure)
                reduce { state.copy(saving = false, message = EntryEditMessage.Unexpected) }
            }
        }
    }

    private suspend fun leave() {
        navigator.navigate(MviNavEvent.NavigateBack)
    }

    /**
     * The country a phone field of none starts with; the countries of the phone fields of [content] and it read
     * ahead, so the form draws them without waiting (pass-docs, Step 77).
     */
    private suspend fun preparePhones(content: EntryContent): String {
        val region = phoneDefaults.region()
        val fields = content.presetFields + content.ownFields
        phones.prepare(fields.mapNotNull { PhoneType.region(it.type) }.toSet() + region)
        return region
    }

    private fun EntryEditState.withPhoneDigits(target: PhoneTarget, digits: String): EntryEditState {
        val field = phoneField(target) ?: return this
        val region = regionOf(field) ?: return this
        val value = phones.e164(region, digits)
        val edited = withPhoneField(target) { it.copy(value = value, type = PhoneType.of(region)) }
        return if (target is PhoneTarget.Preset) {
            edited.copy(problems = problems.filterNot { it is FieldProblem.Required && it.key == target.key })
        } else {
            edited
        }
    }

    // The digits of the number stay, the calling code is of the country now (pass-docs, Step 77, decision 10)
    private fun EntryEditState.withPhoneCountry(target: PhoneTarget, region: String): EntryEditState {
        val field = phoneField(target) ?: return this
        val digits = regionOf(field)?.let { phones.digits(field.value, it) }.orEmpty()
        return withPhoneField(target) { it.copy(value = phones.e164(region, digits), type = PhoneType.of(region)) }
    }

    /**
     * The entry [id] deleted: back to the list it was opened from - past its reading too, when the form was
     * opened from it, so that screen does not flash on the way (pass-docs, Step 50, item 2).
     */
    private suspend fun leaveDeleted(id: String) {
        navigator.navigate(MviNavEvent.NavigateBackTo(EntryReadRoute(dbKey, id), inclusive = true, orBack = true))
    }

    private companion object {
        const val TAG = "EntryEditProcessor"
    }
}

/** The form takes changes: it is read and nothing is under way. */
private val EntryEditState.editable: Boolean get() = content != null && !saving

private fun EntryEditState.ownField(index: Int): EntryField? = content?.ownFields?.getOrNull(index)

/** The country of the phone field [field]: its own, or - none yet - the one by default. */
internal fun EntryEditState.regionOf(field: EntryField): String? = PhoneType.region(field.type) ?: phoneRegion

private fun EntryEditState.phoneField(target: PhoneTarget): EntryField? = when (target) {
    is PhoneTarget.Preset -> content?.presetFields?.firstOrNull { it.key == target.key }
    is PhoneTarget.Own -> ownField(target.index)
}?.takeIf { FieldType.of(it.type) == FieldType.PHONE }

private fun EntryEditState.withPhoneField(target: PhoneTarget, change: (EntryField) -> EntryField): EntryEditState =
    edited {
        when (target) {
            is PhoneTarget.Preset -> copy(presetFields = presetFields.map { if (it.key == target.key) change(it) else it })
            is PhoneTarget.Own -> copy(ownFields = ownFields.mapIndexed { i, field -> if (i == target.index) change(field) else field })
        }
    }

// A phone of no country yet - picked just now in the dialog - takes the one by default
private fun String?.withRegion(region: String?): String? =
    if (FieldType.of(this) == FieldType.PHONE && PhoneType.region(this) == null && region != null) PhoneType.of(region) else this

private inline fun EntryEditState.edited(change: EntryContent.() -> EntryContent): EntryEditState =
    if (content == null || saving) this else copy(content = content.change())

/** The own field at [index] is gone: what is said about the fields after it moves up by one. */
private fun EntryEditState.withoutOwnField(index: Int): EntryEditState = copy(
    shownOwn = shownOwn.filter { it != index }.map { if (it > index) it - 1 else it }.toSet(),
    problems = problems.mapNotNull { problem ->
        when {
            problem !is FieldProblem.BadName -> problem
            problem.index == index -> null
            problem.index > index -> problem.copy(index = problem.index - 1)
            else -> problem
        }
    },
)

private fun List<FieldProblem>.withoutName(index: Int) = filterNot { it is FieldProblem.BadName && it.index == index }

private fun EntryEditDialog.typed(name: String): EntryEditDialog = when (this) {
    is EntryEditDialog.AddField -> copy(name = name, problem = null)
    is EntryEditDialog.EditField -> copy(name = name, problem = null)
    else -> this
}

private fun EntryEditDialog.switched(protected: Boolean): EntryEditDialog = when (this) {
    is EntryEditDialog.AddField -> copy(protected = protected)
    is EntryEditDialog.EditField -> copy(protected = protected)
    else -> this
}

// The kind picked again keeps what the type carries - the country of a phone (pass-docs, Step 77)
private fun EntryEditDialog.typedAs(type: FieldType): EntryEditDialog = when (this) {
    is EntryEditDialog.AddField -> copy(type = this.type.retyped(type))
    is EntryEditDialog.EditField -> copy(type = this.type.retyped(type))
    else -> this
}

// By the key itself: a type this app does not know, shown as text, becomes text when text is picked
private fun String?.retyped(type: FieldType): String? = if (this?.substringBefore(':') == type.key) this else type.key

// The value of one type is no value of another: a new type starts the field empty (pass-docs, Step 75, item 3)
private fun EntryField.edited(name: String, protected: Boolean, type: String?): EntryField =
    copy(key = name, protected = protected, type = type, value = if (type == this.type) value else "")
