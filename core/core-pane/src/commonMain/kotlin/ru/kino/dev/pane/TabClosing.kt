package ru.kino.dev.pane

/**
 * Whether one more tab opens next to [open] tabs, [limit] the most a container keeps (pass-docs, Step 99, item 10).
 * None closes by itself to make room - the person closes one; a tab open already is only shown, it is not counted.
 */
fun canOpenTab(open: Int, limit: Int): Boolean = open < limit

/** The tabs of a container, and the key of the one shown - null: none. */
data class ShownTabs<T>(val tabs: List<T>, val active: String?)

/**
 * The tab [key] closed (pass-docs, Step 98, `docs/dev/navigation.md`): shown instead - the next one, else the one
 * before; another one shown - it stays. Not among them - null.
 */
fun <T> closeTab(tabs: List<T>, active: String?, key: String, keyOf: (T) -> String): ShownTabs<T>? {
    val index = tabs.indexOfFirst { keyOf(it) == key }
    if (index < 0) return null
    val left = tabs - tabs[index]
    val shown = if (active != key) active else left.getOrNull(index)?.let(keyOf) ?: left.lastOrNull()?.let(keyOf)
    return ShownTabs(left, shown)
}
