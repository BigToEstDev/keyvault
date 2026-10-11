package ru.kino.dev.core

/**
 * [NativeCore] in memory, for tests of the repositories built on it.
 *
 * Every call is written to [journal] as `"<method> <dbKey>"` - share the journal with a [FakeDatabaseFiles]
 * to check the order of calls across both. A failure is set per method in [failures] and thrown each time
 * that method is called, after it is journaled and its arguments recorded - the way the real core has them
 * before it refuses.
 */
class FakeNativeCore(val journal: MutableList<String> = mutableListOf()) : NativeCore {

    /** Method name to the failure it throws: a [CoreException] for the core's own, anything else for a bug. */
    val failures = mutableMapOf<String, Throwable>()

    /** Databases the core holds, by dbKey. */
    val opened = mutableMapOf<String, OpenedDatabase>()

    /** Databases locked while open, by dbKey: still in [opened], unlocked again by [unlockDatabase]. */
    val locked = mutableSetOf<String>()

    /** Databases whose file no longer matches the checksum - written by somebody else. */
    val changedElsewhere = mutableSetOf<String>()

    /** The name a database opened or unlocked here reports from inside. */
    var databaseName = "Passwords"

    /** What [createDatabase] and [saveDatabase] give back to be written. */
    var databaseBytes = byteArrayOf(0x03, 0xD9.toByte(), 0xA2.toByte(), 0x9A.toByte())

    var generatedKeyFile = "<KeyFile/>".toByteArray()

    var mergeSummary = MergeSummary(
        addedGroups = 0,
        updatedGroups = 0,
        movedGroups = 0,
        addedEntries = 1,
        updatedEntries = 0,
        movedEntries = 0,
        deletedGroups = 0,
        deletedEntries = 0,
        metaDataChanged = false,
        mergeDone = true,
        differentDatabases = false,
    )

    /** The last [NewDatabase] asked for. */
    var created: NewDatabase? = null

    /** The bytes handed to [openDatabase], [verifyFileChecksum] and [mergeDatabase], last one wins. */
    var bytesReceived: ByteArray? = null

    /** The password of the last open, unlock or create. */
    var passwordReceived: String? = null

    /**
     * Every key file handed in, as the same instances: a test checks after the call that the caller wiped
     * them.
     */
    val keyFilesReceived = mutableListOf<KeyFile>()

    override suspend fun buildInfo(): String = "fake"

    override suspend fun createDatabase(database: NewDatabase): CreatedDatabase {
        created = database
        passwordReceived = database.password
        database.keyFile?.let { keyFilesReceived += it }
        step("createDatabase", database.dbKey)

        val opened = OpenedDatabase(
            dbKey = database.dbKey,
            databaseName = database.databaseName,
            fileName = database.fileName,
            keyFileName = database.keyFile?.name,
        )
        this.opened[database.dbKey] = opened
        return CreatedDatabase(database = opened, bytes = databaseBytes.copyOf())
    }

    override suspend fun openDatabase(
        dbKey: String,
        bytes: ByteArray,
        password: String?,
        fileName: String?,
        keyFile: KeyFile?,
    ): OpenedDatabase {
        bytesReceived = bytes.copyOf()
        passwordReceived = password
        keyFile?.let { keyFilesReceived += it }
        step("openDatabase", dbKey)

        return OpenedDatabase(dbKey, databaseName, fileName, keyFile?.name).also { opened[dbKey] = it }
    }

    override suspend fun lockDatabase(dbKey: String) {
        step("lockDatabase", dbKey)
        if (dbKey !in opened) throw CoreException("DbKeyNotFound", "not open: $dbKey")
        locked += dbKey
    }

    override suspend fun unlockDatabase(dbKey: String, password: String?, keyFile: KeyFile?): OpenedDatabase {
        passwordReceived = password
        keyFile?.let { keyFilesReceived += it }
        step("unlockDatabase", dbKey)

        val database = opened[dbKey] ?: throw CoreException("DbKeyNotFound", "not open: $dbKey")
        locked -= dbKey
        return database
    }

    override suspend fun saveDatabase(dbKey: String): ByteArray {
        step("saveDatabase", dbKey)
        return databaseBytes.copyOf()
    }

    override suspend fun closeDatabase(dbKey: String) {
        step("closeDatabase", dbKey)
        opened.remove(dbKey)
        locked -= dbKey
    }

    override suspend fun verifyFileChecksum(dbKey: String, bytes: ByteArray) {
        step("verifyFileChecksum", dbKey)
        bytesReceived = bytes.copyOf()
        if (dbKey in changedElsewhere) throw CoreException("DbFileContentChangeDetected", "changed: $dbKey")
    }

    override suspend fun mergeDatabase(dbKey: String, bytes: ByteArray): MergeSummary {
        step("mergeDatabase", dbKey)
        bytesReceived = bytes.copyOf()
        return mergeSummary
    }

    override suspend fun generatePassword(options: PasswordOptions): String {
        step("generatePassword", "")
        return "x".repeat(options.length)
    }

    override suspend fun generateKeyFile(): ByteArray {
        step("generateKeyFile", "")
        return generatedKeyFile.copyOf()
    }

    /**
     * The groups of every database, by dbKey, as [groupTree] answers them. The group calls change them the
     * way the core would; a database without a tree here is one the core does not hold.
     */
    val groupTrees = mutableMapOf<String, GroupTree>()

    /** Ids handed to the groups [createGroup] makes - `group-1`, `group-2`, … */
    private var groupsCreated = 0

    override suspend fun groupTree(dbKey: String): GroupTree {
        step("groupTree", dbKey)
        return tree(dbKey)
    }

    override suspend fun createGroup(dbKey: String, parentId: String, name: String): String {
        step("createGroup", dbKey)
        val tree = tree(dbKey)
        val parent = tree.groups[parentId] ?: throw notFound(parentId)

        val id = "group-${++groupsCreated}"
        groupTrees[dbKey] = tree.copy(
            groups = tree.groups +
                (id to GroupNode(id, parentId, name, emptyList(), emptyList())) +
                (parentId to parent.copy(groupIds = parent.groupIds + id)),
        )
        return id
    }

    override suspend fun renameGroup(dbKey: String, groupId: String, name: String) {
        step("renameGroup", dbKey)
        val tree = tree(dbKey)
        val group = tree.groups[groupId] ?: throw notFound(groupId)

        groupTrees[dbKey] = tree.copy(groups = tree.groups + (groupId to group.copy(name = name)))
    }

    override suspend fun moveGroupToRecycleBin(dbKey: String, groupId: String) {
        step("moveGroupToRecycleBin", dbKey)
        var tree = tree(dbKey)
        val group = tree.groups[groupId] ?: throw notFound(groupId)

        // Like the core: no bin yet - one is made under the root on the first delete
        val binId = tree.recycleBinId ?: RECYCLE_BIN_ID.also { binId ->
            val root = tree.groups.getValue(tree.rootId)
            tree = tree.copy(
                recycleBinId = binId,
                groups = tree.groups +
                    (binId to GroupNode(binId, tree.rootId, "Recycle Bin", emptyList(), emptyList())) +
                    (tree.rootId to root.copy(groupIds = root.groupIds + binId)),
            )
        }
        val oldParent = tree.groups.getValue(checkNotNull(group.parentId) { "the root cannot be deleted" })
        val bin = tree.groups.getValue(binId)

        groupTrees[dbKey] = tree.copy(
            groups = tree.groups +
                (oldParent.id to oldParent.copy(groupIds = oldParent.groupIds - groupId)) +
                (binId to bin.copy(groupIds = bin.groupIds + groupId)) +
                (groupId to group.copy(parentId = binId)),
        )
    }

    private fun tree(dbKey: String): GroupTree =
        groupTrees[dbKey] ?: throw CoreException("DbKeyNotFound", "not open: $dbKey")

    private fun notFound(id: String) = CoreException("NotFound", "no group $id")

    private fun step(method: String, dbKey: String) {
        journal += "$method $dbKey".trimEnd()
        failures[method]?.let { throw it }
    }

    companion object {
        /** The id of the recycle bin [moveGroupToRecycleBin] makes when the database has none. */
        const val RECYCLE_BIN_ID = "recycle-bin"
    }
}
