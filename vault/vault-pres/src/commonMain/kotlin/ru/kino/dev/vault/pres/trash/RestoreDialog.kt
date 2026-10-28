package ru.kino.dev.vault.pres.trash

import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.group.VaultTree
import ru.kino.dev.vault.pres.common.groupChoices

/**
 * "Restore" for entries that came from [previous] groups: every group they can go to, in the order of the tree, and
 * the one they came from offered - when they all share it and it lives (pass-docs, Step 70). The bin and what is in
 * it are not among the groups.
 */
internal fun VaultTree.restoreDialog(previous: List<GroupId?>): TrashDialog.Restore {
    val choices = groupChoices()
    val offered = previous.distinct().singleOrNull()?.takeIf { id -> choices.any { it.id == id } }
    return TrashDialog.Restore(choices, offered)
}
