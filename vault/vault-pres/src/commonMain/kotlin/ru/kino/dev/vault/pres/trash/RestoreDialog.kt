package ru.kino.dev.vault.pres.trash

import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.group.VaultTree
import ru.kino.dev.vault.pres.common.groupChoices

/**
 * "Restore" for entries that came from [previous] groups: every group they can go to, in the order of the tree, and
 * the one they came from offered - when they all share it and it lives (pass-docs, Step 70); else - the root, so a
 * group is always chosen (Step 100, item 4): not known where they came from, gone, from different ones. The bin and
 * what is in it are not among the groups.
 */
internal fun VaultTree.restoreDialog(previous: List<GroupId?>): TrashDialog.Restore {
    val choices = groupChoices()
    val came = previous.distinct().singleOrNull()?.takeIf { id -> choices.any { it.id == id } }
    return TrashDialog.Restore(choices, came ?: choices.firstOrNull()?.id)
}
