package ru.kino.dev.vault.pres.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.kino.dev.core.pres.R
import ru.kino.dev.ui.components.DropdownOption
import ru.kino.dev.vault.group.GroupId
import ru.kino.dev.vault.group.VaultTree

/**
 * A group an entry can be in, as the screens of an entry show it: its name, and the path from the root down
 * to it - the group itself included (pass-docs, Step 48).
 *
 * @param path the names from the root to the group, both ends included; the root's is the database's
 */
data class GroupChoice(val id: GroupId, val path: List<String>) {
    val isRoot: Boolean get() = path.size == 1
}

/**
 * Every group an entry can go into, in the order of the tree: the root, then each group right before the
 * groups inside it. The recycle bin and what is in it are not visible, so not among them.
 */
fun VaultTree.groupChoices(): List<GroupChoice> = (listOf(root) + descendants(rootId)).mapNotNull { groupChoice(it.id) }

/** The group [id] as a choice; null when it is not visible - in the recycle bin, or gone. */
fun VaultTree.groupChoice(id: GroupId): GroupChoice? =
    path(id).takeIf { it.isNotEmpty() }?.let { path -> GroupChoice(id, path.map { it.name }) }

/** The group as the field shows it once chosen - the name; the root's blank one reads "Database". */
@Composable
internal fun GroupChoice.title(): String {
    val name = path.last()
    return if (isRoot) name.ifBlank { stringResource(R.string.entry_database_fallback) } else name
}

/** The path under the name, with the group itself at the end; none for the root - nothing above it. */
@Composable
internal fun GroupChoice.subtitle(): String? {
    if (isRoot) return null
    val fallback = stringResource(R.string.entry_database_fallback)
    return path
        .mapIndexed { index, name -> if (index == 0) name.ifBlank { fallback } else name }
        .joinToString(stringResource(R.string.vault_location_separator))
}

@Composable
internal fun GroupChoice.option(): DropdownOption<GroupId> = DropdownOption(id, title(), subtitle())
