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
        val group = tree(dbKey).groups[groupId] ?: throw notFound(groupId)
        val binId = recycleBin(dbKey)
        val tree = tree(dbKey)
        val oldParent = tree.groups.getValue(checkNotNull(group.parentId) { "the root cannot be deleted" })
        val bin = tree.groups.getValue(binId)

        groupTrees[dbKey] = tree.copy(
            groups = tree.groups +
                (oldParent.id to oldParent.copy(groupIds = oldParent.groupIds - groupId)) +
                (binId to bin.copy(groupIds = bin.groupIds + groupId)) +
                (groupId to group.copy(parentId = binId)),
        )
    }

    /** An entry the fake holds: the group it is in, its strings in order, when it was made. */
    data class FakeEntry(val groupId: String, val fields: List<EntryFieldData>, val createdAt: Long)

    /**
     * The entries of every database, by dbKey and then by id. The entry calls change them - and the
     * [groupTrees] they are named in - the way the core would.
     */
    val storedEntries = mutableMapOf<String, MutableMap<String, FakeEntry>>()

    /** Ids handed to the entries [createEntry] makes - `entry-1`, `entry-2`, … */
    private var entriesCreated = 0

    /**
     * Puts an entry into [groupId] - into the tree too - for setting a test up. Not journaled. Without
     * [createdAt] every entry is a second younger than the one put before it.
     */
    fun addEntry(
        dbKey: String,
        groupId: String,
        id: String,
        fields: List<EntryFieldData>,
        createdAt: Long = ++clock,
    ): String {
        val tree = tree(dbKey)
        val group = tree.groups.getValue(groupId)
        groupTrees[dbKey] = tree.copy(groups = tree.groups + (groupId to group.copy(entryIds = group.entryIds + id)))
        storedEntries.getOrPut(dbKey) { mutableMapOf() }[id] = FakeEntry(groupId, fields, createdAt)
        return id
    }

    /** The creation time handed to the next entry made without one, in seconds. */
    private var clock = 1_700_000_000L

    override suspend fun entries(dbKey: String, groupId: String): List<EntrySummaryData> {
        step("entries", dbKey)
        // Like the core: a group that is not there has no entries rather than a failure
        val group = tree(dbKey).groups[groupId] ?: return emptyList()
        return group.entryIds
            .mapNotNull { id -> stored(dbKey)[id]?.let { summary(id, it) } }
            .sortedBy { it.title }
    }

    // The core's rules, on the fake's entries: title and user name, never a protected one, case aside, the
    // recycle bin and the groups inside it left out; in no particular order - here, the order they were put
    override suspend fun searchEntries(dbKey: String, term: String): EntrySearchData {
        step("searchEntries", dbKey)
        val needle = term.trim().lowercase()
        if (needle.isEmpty()) return EntrySearchData(term, emptyList())

        fun open(entry: FakeEntry, key: String) =
            entry.fields.firstOrNull { it.key == key }?.takeUnless { it.protected }?.value.orEmpty()

        val found = stored(dbKey)
            .filter { (_, entry) -> !inRecycleBin(dbKey, entry.groupId) }
            .filter { (_, entry) -> listOf("Title", "UserName").any { open(entry, it).lowercase().contains(needle) } }
            .map { (id, entry) -> summary(id, entry) }
        return EntrySearchData(term, found)
    }

    private fun inRecycleBin(dbKey: String, groupId: String): Boolean {
        val tree = tree(dbKey)
        val bin = tree.recycleBinId ?: return false
        return generateSequence(groupId) { tree.groups[it]?.parentId }.any { it == bin }
    }

    override suspend fun entryFields(dbKey: String, entryId: String): List<EntryFieldData> {
        step("entryFields", dbKey)
        return entry(dbKey, entryId).fields
    }

    override suspend fun createEntry(dbKey: String, groupId: String, fields: List<EntryFieldData>): String {
        step("createEntry", dbKey)
        tree(dbKey).groups[groupId] ?: throw notFound(groupId)
        refuseDuplicateKeys(fields)
        return addEntry(dbKey, groupId, "entry-${++entriesCreated}", fields)
    }

    override suspend fun updateEntry(dbKey: String, entryId: String, fields: List<EntryFieldData>) {
        step("updateEntry", dbKey)
        val entry = entry(dbKey, entryId)
        refuseDuplicateKeys(fields)
        stored(dbKey)[entryId] = entry.copy(fields = fields)
    }

    override suspend fun moveEntryToRecycleBin(dbKey: String, entryId: String) {
        step("moveEntryToRecycleBin", dbKey)
        val entry = entry(dbKey, entryId)
        val binId = recycleBin(dbKey)
        val tree = tree(dbKey)
        val oldParent = tree.groups.getValue(entry.groupId)
        val bin = tree.groups.getValue(binId)

        groupTrees[dbKey] = tree.copy(
            groups = tree.groups +
                (oldParent.id to oldParent.copy(entryIds = oldParent.entryIds - entryId)) +
                (binId to bin.copy(entryIds = bin.entryIds + entryId)),
        )
        stored(dbKey)[entryId] = entry.copy(groupId = binId)
    }

    private fun stored(dbKey: String): MutableMap<String, FakeEntry> {
        tree(dbKey)
        return storedEntries.getOrPut(dbKey) { mutableMapOf() }
    }

    private fun entry(dbKey: String, entryId: String): FakeEntry =
        stored(dbKey)[entryId] ?: throw CoreException("NotFound", "no entry $entryId")

    private fun summary(id: String, entry: FakeEntry) = EntrySummaryData(
        id = id,
        groupId = entry.groupId,
        title = entry.fields.firstOrNull { it.key == "Title" }?.value.orEmpty(),
        userName = entry.fields.firstOrNull { it.key == "UserName" }?.value.orEmpty(),
        createdAt = entry.createdAt,
    )

    // Like the core since Step 45: a form with two fields under one key is not saved
    private fun refuseDuplicateKeys(fields: List<EntryFieldData>) {
        val keys = fields.map { it.key }
        if (keys.size != keys.toSet().size) throw CoreException("DuplicateFieldKey", "two fields with one key")
    }

    /** The id of the recycle bin; like the core, one is made under the root on the first delete. */
    private fun recycleBin(dbKey: String): String {
        val tree = tree(dbKey)
        tree.recycleBinId?.let { return it }
        val root = tree.groups.getValue(tree.rootId)
        groupTrees[dbKey] = tree.copy(
            recycleBinId = RECYCLE_BIN_ID,
            groups = tree.groups +
                (RECYCLE_BIN_ID to GroupNode(RECYCLE_BIN_ID, tree.rootId, "Recycle Bin", emptyList(), emptyList())) +
                (tree.rootId to root.copy(groupIds = root.groupIds + RECYCLE_BIN_ID)),
        )
        return RECYCLE_BIN_ID
    }

    private fun tree(dbKey: String): GroupTree =
        groupTrees[dbKey] ?: throw CoreException("DbKeyNotFound", "not open: $dbKey")

    private fun notFound(id: String) = CoreException("NotFound", "no group $id")

    private fun step(method: String, dbKey: String) {
        journal += "$method $dbKey".trimEnd()
        failures[method]?.let { throw it }
    }

    companion object {
        /** The id of the recycle bin a move to the bin makes when the database has none. */
        const val RECYCLE_BIN_ID = "recycle-bin"
    }
}
