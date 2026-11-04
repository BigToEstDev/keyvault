package ru.kino.dev.settings.pres.presetscontainer

/**
 * What the container of the presets holds on a wide window (pass-docs, Step 100, item 3): the presets open on the
 * right, as tabs - each its form; the list of the presets on the left is always the same.
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

    /** The panes to keep - the list and the tabs; the rest are cleared. */
    val paneKeys: Set<String> = setOf(PRESETS_LIST_PANE) + tabs.map { it.key }
}

/** A tab on the right: the form of a preset. [key] - which tab, and its pane. */
sealed interface PresetTab {
    val key: String

    /** The preset [presetId] edited. [title] - its name; null while it is read. */
    data class Edit(val presetId: String, val title: String? = null) : PresetTab {
        override val key: String = presetPane(presetId)
    }

    /** A new preset made - the tab's own [key], the preset has no id yet. */
    data class New(override val key: String) : PresetTab
}

/** The key of the pane of the list of the presets. */
const val PRESETS_LIST_PANE = "presets"

/** The key of the tab - and of its pane - of the preset [presetId]. */
fun presetPane(presetId: String): String = "preset:$presetId"

/** The key of the tab - and of its pane - of the [number]th new preset made in the container. */
fun newPresetTab(number: Int): String = "new:$number"
