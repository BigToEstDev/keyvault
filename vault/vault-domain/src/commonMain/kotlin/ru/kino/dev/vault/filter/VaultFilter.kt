package ru.kino.dev.vault.filter

/**
 * How the list of `vault` is shown - one choice for the whole app, kept across launches (pass-docs,
 * `docs/android/screens/vault/vault-our.md`, "Диалог фильтров", "Время жизни Фильтра").
 *
 * The default is what a list looks like with no filter chosen; anything else lights the filter button up.
 */
data class VaultFilter(
    val visibility: Visibility = Visibility.All,
    val order: CreationOrder = CreationOrder.NewestFirst,
) {
    val isDefault: Boolean get() = this == DEFAULT

    companion object {
        val DEFAULT = VaultFilter()
    }
}

/** What the list holds. */
enum class Visibility { All, GroupsOnly, EntriesOnly }

/**
 * The order of the entries, by when they were created. Groups have no time of their own in the tree: they
 * always come first, in the order of the database.
 */
enum class CreationOrder { NewestFirst, OldestFirst }
