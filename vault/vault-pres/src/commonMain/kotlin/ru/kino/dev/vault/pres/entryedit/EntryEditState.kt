package ru.kino.dev.vault.pres.entryedit

import ru.kino.dev.core.FieldProtection
import ru.kino.dev.core.phone.PhoneCountry
import ru.kino.dev.core.preset.EntryPreset
import ru.kino.dev.core.preset.PresetId
import ru.kino.dev.vault.entry.EntryContent
import ru.kino.dev.vault.entry.EntryDraft
import ru.kino.dev.vault.entry.EntryField
import ru.kino.dev.vault.entry.EntryValidator
import ru.kino.dev.vault.entry.FieldProblem
import ru.kino.dev.vault.entry.NameProblem
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.pres.common.GroupChoice

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
    val changed: Boolean = content != initial || group != initialGroup

    /** One more own field can be added - the card of own fields has room (pass-docs, Step 40, fix 19.1). */
    val canAddField: Boolean = content?.let(EntryValidator::canAddField) == true

    /** The form takes changes: it is read and nothing is under way. */
    internal val editable: Boolean = content != null && !saving

    /** The required field of the preset under [key] was refused for being empty. */
    fun isMissing(key: String): Boolean = problems.any { it is FieldProblem.Required && it.key == key }

    /** Why the name of the own field at [index] was refused, if it was. */
    fun nameProblem(index: Int): NameProblem? =
        problems.firstNotNullOfOrNull { (it as? FieldProblem.BadName)?.takeIf { bad -> bad.index == index }?.problem }
}

/** The dialog over the form, if any. */
sealed interface EntryEditDialog {

    /**
     * A new own field: its name, whether it is protected - not, until switched - and its [type], as the file keeps
     * it - text, until picked (pass-docs, Step 75).
     */
    data class AddField(
        val name: String = "",
        val protected: Boolean = false,
        val type: String? = null,
        val problem: NameProblem? = null,
    ) : EntryEditDialog

    /**
     * The own field at [index] changed: its name, whether it is protected and its [type], opened as they are - a
     * type this app does not know stays, until another is picked. Another type clears the value (Step 75, item 3).
     */
    data class EditField(
        val index: Int,
        val name: String,
        val protected: Boolean,
        val type: String?,
        val problem: NameProblem? = null,
    ) : EntryEditDialog

    /** "Delete the field?" - the own field at [index], called [name]. */
    data class DeleteField(val index: Int, val name: String) : EntryEditDialog

    /** The country of the phone field [target]: [countries] to choose from, [selected] - the one it has. */
    data class PhoneCountries(
        val target: PhoneTarget,
        val selected: String,
        val countries: List<PhoneCountry>,
    ) : EntryEditDialog

    /**
     * The choice of presets: the standard one, then [presets] - those of one's own, in the order they were
     * made; [selected] is the one of the form, null for the standard one.
     */
    data class Preset(val presets: List<EntryPreset>, val selected: PresetId?) : EntryEditDialog

    /**
     * "Save" with fields hidden by the preset chosen - their [keys]: "save as is" (confirm) drops them, "add
     * outside the preset" ([EntryEditActions.onKeepHidden]) keeps them as own fields, "cancel" stays.
     */
    data class HiddenFields(val keys: List<String>) : EntryEditDialog

    /** "Leave without saving?" - "back" with changes. */
    data object Leave : EntryEditDialog

    /**
     * "Delete the entry?" - called [title] as the file holds it; what is typed goes with it, unasked
     * (pass-docs, Step 50, item 2).
     *
     * @param toBin the delete goes to the recycle bin; false - for good (pass-docs, Step 70)
     * @param deleting the delete is under way: the dialog shows progress and takes nothing
     */
    data class Delete(val title: String, val toBin: Boolean, val deleting: Boolean = false) : EntryEditDialog

    /** The change is made but the file is not written; the screen closes after it. */
    data object NotSaved : EntryEditDialog
}

/** What the snackbar says. */
enum class EntryEditMessage { LoadFailed, NotSaved, Unexpected }

/** A phone field of the form: of the preset - by its key, or own - by its index. */
sealed interface PhoneTarget {
    data class Preset(val key: String) : PhoneTarget
    data class Own(val index: Int) : PhoneTarget
}
