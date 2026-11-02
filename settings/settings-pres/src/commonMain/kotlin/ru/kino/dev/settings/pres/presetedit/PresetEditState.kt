package ru.kino.dev.settings.pres.presetedit

import kotlinx.coroutines.flow.first
import ru.kino.dev.core.preset.CustomPreset
import ru.kino.dev.core.preset.FieldNameProblem
import ru.kino.dev.core.preset.FieldPosition
import ru.kino.dev.core.preset.PresetNameProblem
import ru.kino.dev.core.preset.PresetProblem
import ru.kino.dev.core.preset.PresetValidator

/**
 * @param name the name of the preset, as typed
 * @param cards the names of the fields of each card, as [ru.kino.dev.core.preset.CustomPreset.of] takes them:
 * the first - what goes after the user name and the password; the rest - the cards the person added
 * @param protectedKeys the names of the fields of [cards] that are protected in a new entry (pass-docs, Step 75)
 * @param fieldTypes the types of the fields of [cards] in a new entry, by name - text is none (Step 75, item 3)
 * @param problems what the last save was refused for, until that part is edited
 * @param saving the preset is being made - the fab waits, the form takes nothing
 * @param editing a preset that is there is edited, not a new one made
 * @param loading the preset to edit is read - the fab waits, the form takes nothing
 * @param initialName the name as read; empty for a new preset
 * @param initialCards the cards as read; the first card alone for a new preset
 * @param initialProtectedKeys the protected fields as read; none for a new preset
 * @param initialFieldTypes the types of the fields as read; none for a new preset
 * @param message for the snackbar, until it takes it
 */
data class PresetEditState(
    val name: String = "",
    val cards: List<List<String>> = EMPTY_CARDS,
    val protectedKeys: Set<String> = emptySet(),
    val fieldTypes: Map<String, String> = emptyMap(),
    val problems: List<PresetProblem> = emptyList(),
    val saving: Boolean = false,
    val editing: Boolean = false,
    val loading: Boolean = false,
    val initialName: String = "",
    val initialCards: List<List<String>> = EMPTY_CARDS,
    val initialProtectedKeys: Set<String> = emptySet(),
    val initialFieldTypes: Map<String, String> = emptyMap(),
    val dialog: PresetEditDialog? = null,
    val message: PresetEditMessage? = null,
) {
    /** Anything typed, added, removed or switched since it was read: "back" asks first. */
    val changed: Boolean = name != initialName || cards != initialCards || protectedKeys != initialProtectedKeys ||
            fieldTypes != initialFieldTypes

    /**
     * A name of a field read is gone - renamed, deleted, gone with its card: entries with the preset will show
     * their values of it as their own fields (pass-docs, Step 62).
     */
    val dropsFields: Boolean = (initialCards.flatten() - cards.flatten().toSet()).isNotEmpty()

    /** One more field can go into the card [card] - it has room (pass-docs, Step 40, fix 19.1). */
    fun canAddField(card: Int): Boolean = PresetValidator.canAddField(cards, card)

    /** One more card can be added - the preset has room (pass-docs, Step 40, fix 19.2). */
    val canAddCard: Boolean = PresetValidator.canAddCard(cards)

    /** Why the name was refused, if it was. */
    val nameProblem: PresetNameProblem? = problems.firstNotNullOfOrNull { (it as? PresetProblem.Name)?.problem }

    /** Why the name of the field [at] was refused, if it was. */
    fun fieldProblem(at: FieldPosition): FieldNameProblem? =
        problems.firstNotNullOfOrNull { (it as? PresetProblem.FieldName)?.takeIf { bad -> bad.at == at }?.problem }

    private companion object {
        // The first card is always there: the user name and the password are in it
        val EMPTY_CARDS = listOf(emptyList<String>())
    }
}

/** The dialog over the form, if any. */
sealed interface PresetEditDialog {

    /**
     * A new field of the card [card]: its name, whether it is protected - not, until switched - and its [type] -
     * text, until picked (Step 75).
     */
    data class AddField(
        val card: Int,
        val name: String = "",
        val protected: Boolean = false,
        val type: String? = null,
        val problem: FieldNameProblem? = null,
    ) : PresetEditDialog

    /** The field [at] changed: its name, whether it is protected and its [type], opened as they are. */
    data class EditField(
        val at: FieldPosition,
        val name: String,
        val protected: Boolean,
        val type: String?,
        val problem: FieldNameProblem? = null,
    ) : PresetEditDialog

    /** "Delete the field?" - the field [at], called [name]. */
    data class DeleteField(val at: FieldPosition, val name: String) : PresetEditDialog

    /** "Delete the card? Its fields go too." */
    data class DeleteCard(val card: Int) : PresetEditDialog

    /** "Leave without saving?" - "back" with changes. */
    data object Leave : PresetEditDialog

    /**
     * "Entries with this preset will show the old fields as their own" - a change of a preset that drops names of
     * its fields: renamed, deleted, gone with a card (pass-docs, Step 62). [standardName] - for the save it leads to.
     */
    data class FieldsDropped(val standardName: String) : PresetEditDialog
}

/** What the snackbar says. */
enum class PresetEditMessage {
    /** There are ten presets already, the standard one included */
    TooMany,

    /** The store could not be written; nothing is made, the form stays */
    NotSaved,

    /** The preset edited is gone - deleted on another screen; nothing is saved */
    Gone,
}
