package ru.kino.dev.pane

/**
 * The routers of the panes of one container, by the key of the pane (pass-docs, Step 99, item 9). The navigator of a
 * pane is given to the processors of its screen once, when they are made, and kept by them; so it asks the router its
 * own pane was last drawn with - not the one of the place in the layout the pane is drawn at: by then that place may
 * draw another pane, or be gone.
 */
class PaneRouters {

    private val routers = mutableMapOf<String, PaneRouter>()

    /** The pane [key] is drawn with [router]. */
    fun set(key: String, router: PaneRouter) {
        routers[key] = router
    }

    /**
     * What the pane [key] navigates through: the router it was last drawn with. A pane gone - its navigation, late, is
     * dropped: it is the container's, and going on it would leave the container.
     */
    fun router(key: String): PaneRouter = PaneRouter { event -> routers[key]?.route(event) ?: true }

    /** Keeps the routers of the panes of [keys], forgets the rest. */
    fun retain(keys: Set<String>) {
        routers.keys.retainAll(keys)
    }
}
