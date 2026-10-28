package ru.kino.dev.vault.group

/**
 * The rules of a group name, for both sides: the dialog shows the problem under the field, [GroupEditor]
 * refuses the change.
 *
 * Checked only for names this app writes. A group read from another program's file keeps its name as it
 * is - longer, or a twin of its neighbour - and nothing here touches it.
 */
object GroupNameValidator {

    /** Longest name, in characters (code points, so an emoji is one). Decided by the owner, Step 36. */
    const val MAX_LENGTH = 256

    /** The name as it is stored: without the spaces around it. */
    fun normalize(name: String): String = name.trim()

    /**
     * The first problem with [name] as the name of a group inside [parentId], or null when there is none.
     *
     * @param renamedId the group being renamed - its current name is not a rival
     */
    fun problem(tree: VaultTree, parentId: GroupId, name: String, renamedId: GroupId? = null): GroupFailure? {
        val normalized = normalize(name)
        return when {
            normalized.isEmpty() -> GroupFailure.NameEmpty()
            normalized.codePointCount(0, normalized.length) > MAX_LENGTH -> GroupFailure.NameTooLong()
            tree.isNameTaken(parentId, normalized, renamedId) -> GroupFailure.NameTaken()
            else -> null
        }
    }
}
