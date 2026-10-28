package ru.kino.dev.vault.group

/*
 * A tree for tests, written the way it is drawn:
 *
 * root (1 entry)
 * ├── Work (2 entries)
 * │   └── Mail (3 entries)
 * ├── Home (no entries)
 * └── Recycle Bin (1 entry)
 *     └── Old (4 entries)
 */

val ROOT = GroupId("root")
val WORK = GroupId("work")
val MAIL = GroupId("mail")
val HOME = GroupId("home")
val BIN = GroupId("bin")
val OLD = GroupId("old")

fun sampleTree(): VaultTree = VaultTree(
    rootId = ROOT,
    recycleBinId = BIN,
    groups = listOf(
        group(ROOT, null, "Passwords", children = listOf(WORK, HOME, BIN), entries = 1),
        group(WORK, ROOT, "Work", children = listOf(MAIL), entries = 2),
        group(MAIL, WORK, "Mail", entries = 3),
        group(HOME, ROOT, "Home"),
        group(BIN, ROOT, "Recycle Bin", children = listOf(OLD), entries = 1),
        group(OLD, BIN, "Old", entries = 4),
    ).associateBy { it.id },
)

fun group(
    id: GroupId,
    parentId: GroupId?,
    name: String,
    children: List<GroupId> = emptyList(),
    entries: Int = 0,
) = VaultGroup(
    id = id,
    parentId = parentId,
    name = name,
    childGroupIds = children,
    entryIds = List(entries) { EntryId("${id.value}-entry-$it") },
)
