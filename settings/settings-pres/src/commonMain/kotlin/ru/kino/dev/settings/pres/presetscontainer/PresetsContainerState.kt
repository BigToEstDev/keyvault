package ru.kino.dev.settings.pres.presetscontainer

/**
 * What the container of the presets holds on a wide window (pass-docs, Step 100, item 3): the presets open on the
 * right, as tabs; the list of the presets on the left is always the same.
 *
 * @param tabs the presets open, in the order they were opened
 * @param active the key of the tab shown; null - none open
 */
data class PresetsContainerState(
    val tabs: List<PresetTab> = emptyList(),
    val active: String? = null,
) {
    /** The tab shown; null - none. */
    val shown: PresetTab? = tabs.firstOrNull { it.key == active }

    /** The tabs of new presets - not saved yet, each takes a place among the presets (pass-docs, Step 99, item 7). */
    val drafts: Int = tabs.count { it is PresetTab.New }

    /** The panes to keep - the list and the tabs, the reading under a form too; the rest are cleared. */
    val paneKeys: Set<String> = setOf(PRESETS_LIST_PANE) + tabs.flatMap { it.panes }
}

/** A tab on the right - one page shown at a time, as a tab of an entry of `vault` (pass-docs, Step 99, item 8). */
sealed interface PresetTab {
    /** Which tab. */
    val key: String

    /** The pane shown in the tab. */
    val pane: String

    /** The panes of the tab to keep. */
    val panes: List<String>

    /**
     * The preset [presetId] - null for the standard one: read, or its form over the reading, [editing]. [title] - its
     * name; null while it is read.
     */
    data class Preset(val presetId: String?, val title: String? = null, val editing: Boolean = false) : PresetTab {
        override val key: String = presetPane(presetId)
        override val pane: String = if (editing) presetFormPane(presetId) else key
        override val panes: List<String> = if (editing) listOf(key, pane) else listOf(key)
    }

    /**
     * A new preset made - its form; [key] - the tab's own, the preset has no id yet. [readSaved] - made from the list:
     * saved, the tab reads it.
     */
    data class New(override val key: String, val readSaved: Boolean = false) : PresetTab {
        override val pane: String = key
        override val panes: List<String> = listOf(key)
    }
}

/** The key of the pane of the list of the presets. */
const val PRESETS_LIST_PANE = "presets"

/** The key of the tab of the preset [presetId] - null for the standard one - and of the pane of its reading. */
fun presetPane(presetId: String?): String = "preset:${presetId ?: STANDARD}"

/** The key of the pane of the form of the preset [presetId], over its reading. */
fun presetFormPane(presetId: String?): String = "preset-form:${presetId ?: STANDARD}"

/** The key of the tab - and of its pane - of the [number]th new preset made in the container. */
fun newPresetTab(number: Int): String = "new:$number"

private const val STANDARD = "standard"
