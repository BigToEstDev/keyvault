package ru.kino.dev.core

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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

    /** The bytes [setFileChecksum] took as the file, by dbKey. */
    val checksumBytes = mutableMapOf<String, ByteArray>()

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

    /**
     * The bytes handed to [openDatabase], [verifyFileChecksum], [mergeDatabase], [mergeCopy],
     * [mergeCopyBothWays] and [importDatabase], last one wins.
     */
    var bytesReceived: ByteArray? = null

    /** The password of the last open, unlock, create, merge of a copy or import. */
    var passwordReceived: String? = null

    /** The password [verifyPassword] takes, of every database here. */
    var password: String? = "pw"

    /** What [credentialsUsed] answers, of every database here. */
    var credentials = CredentialsUsed(password = true, keyFile = false)

    /** What [databaseFile] answers, of every database here; [changeDatabaseFile] changes it. */
    var databaseFile = DatabaseFile(name = "Passwords", version = KdbxVersion.V41, compression = Compression.GZip)

    /** What [databaseEntries] answers, of every database here; [changeDatabaseEntries] replaces it. */
    var databaseEntries = DatabaseEntries(FieldProtection.DEFAULT, HistoryLimits(maxItems = 10, maxSize = 6L * 1024 * 1024))

    /** The encryption [databaseSecurity] answers, of every database here; [changeDatabaseEncryption] replaces it. */
    var encryption = Encryption(Cipher.Aes256, KdfVariant.Argon2id, iterations = 3)

    /** The key file name [databaseSecurity] answers; [changeDatabaseCredentials] changes it with [credentials]. */
    var keyFileName: String? = null

    /** What [measureKdfPass] answers - about a pass on the phone of pass-docs `kdf-time.md`. */
    var kdfPass: Duration = 850.milliseconds

    /** `Meta/RecycleBinEnabled` of the open database; off - a delete is for good. */
    var recycleBinEnabled = true

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

    override suspend fun verifyPassword(dbKey: String, password: String): Boolean {
        step("verifyPassword", dbKey)
        if (dbKey !in opened) throw CoreException("DbKeyNotFound", "not open: $dbKey")
        return this.password == password
    }

    override suspend fun credentialsUsed(dbKey: String): CredentialsUsed {
        step("credentialsUsed", dbKey)
        if (dbKey !in opened) throw CoreException("DbKeyNotFound", "not open: $dbKey")
        return credentials
    }

    override suspend fun databaseFile(dbKey: String): DatabaseFile {
        step("databaseFile", dbKey)
        if (dbKey !in opened) throw CoreException("DbKeyNotFound", "not open: $dbKey")
        return databaseFile
    }

    override suspend fun changeDatabaseFile(dbKey: String, name: String, compression: Compression) {
        step("changeDatabaseFile", dbKey)
        if (dbKey !in opened) throw CoreException("DbKeyNotFound", "not open: $dbKey")
        databaseFile = databaseFile.copy(name = name, compression = compression)
    }

    override suspend fun databaseEntries(dbKey: String): DatabaseEntries {
        step("databaseEntries", dbKey)
        if (dbKey !in opened) throw CoreException("DbKeyNotFound", "not open: $dbKey")
        return databaseEntries
    }

    override suspend fun changeDatabaseEntries(dbKey: String, entries: DatabaseEntries) {
        step("changeDatabaseEntries", dbKey)
        if (dbKey !in opened) throw CoreException("DbKeyNotFound", "not open: $dbKey")
        databaseEntries = entries
    }

    override suspend fun databaseSecurity(dbKey: String): DatabaseSecurity {
        step("databaseSecurity", dbKey)
        if (dbKey !in opened) throw CoreException("DbKeyNotFound", "not open: $dbKey")
        return DatabaseSecurity(
            encryption = encryption,
            memoryBytes = 64L * 1024 * 1024,
            parallelism = 2,
            credentials = credentials,
            keyFileName = keyFileName,
        )
    }

    override suspend fun changeDatabaseEncryption(dbKey: String, encryption: Encryption) {
        step("changeDatabaseEncryption", dbKey)
        if (dbKey !in opened) throw CoreException("DbKeyNotFound", "not open: $dbKey")
        this.encryption = encryption
    }

    /** As the core: both factors gone is refused before anything changes. */
    override suspend fun changeDatabaseCredentials(dbKey: String, change: CredentialsChange) {
        if (change is CredentialsChange.SetPassword) passwordReceived = change.password
        if (change is CredentialsChange.SetKeyFile) keyFilesReceived += change.keyFile
        step("changeDatabaseCredentials", dbKey)
        if (dbKey !in opened) throw CoreException("DbKeyNotFound", "not open: $dbKey")

        when (change) {
            is CredentialsChange.SetPassword -> {
                password = change.password
                credentials = credentials.copy(password = true)
            }
            is CredentialsChange.SetKeyFile -> {
                keyFileName = change.keyFile.name
                credentials = credentials.copy(keyFile = true)
            }
            CredentialsChange.RemoveKeyFile -> {
                if (!credentials.password) throw CoreException("InSufficientCredentials", "no password left")
                keyFileName = null
                credentials = credentials.copy(keyFile = false)
            }
        }
    }

    override suspend fun measureKdfPass(dbKey: String): Duration {
        step("measureKdfPass", dbKey)
        if (dbKey !in opened) throw CoreException("DbKeyNotFound", "not open: $dbKey")
        return kdfPass
    }

    override suspend fun recycleBinEnabled(dbKey: String): Boolean {
        step("recycleBinEnabled", dbKey)
        tree(dbKey)
        return recycleBinEnabled
    }

    // Like the core: switched off, the bin goes with all it holds
    override suspend fun changeRecycleBinEnabled(dbKey: String, enabled: Boolean) {
        step("changeRecycleBinEnabled", dbKey)
        val tree = tree(dbKey)
        if (enabled == recycleBinEnabled) return
        recycleBinEnabled = enabled
        val binId = tree.recycleBinId ?: return
        if (enabled) return
        removeGroups(dbKey, subtree(tree, binId))
        groupTrees[dbKey] = groupTrees.getValue(dbKey).copy(recycleBinId = null)
    }

    override suspend fun trashEntries(dbKey: String): List<TrashEntryData> {
        step("trashEntries", dbKey)
        return trashed(dbKey).map { (id, entry) -> trashEntry(dbKey, id, entry) }
    }

    override suspend fun searchTrash(dbKey: String, term: String): TrashSearchData {
        step("searchTrash", dbKey)
        val found = trashed(dbKey)
            .filter { (_, entry) -> entry.matches(term) }
            .map { (id, entry) -> trashEntry(dbKey, id, entry) }
        return TrashSearchData(term, found)
    }

    override suspend fun restoreEntries(dbKey: String, entryIds: List<String>, groupId: String) {
        step("restoreEntries", dbKey)
        val tree = tree(dbKey)
        if (groupId !in tree.groups) throw notFound(groupId)
        if (inRecycleBin(dbKey, groupId)) throw CoreException("DataError", "no restoring into the bin")
        refuseOutsideBin(dbKey, entryIds)
        entryIds.distinct().forEach { relocate(dbKey, it, groupId) }
    }

    override suspend fun removeEntries(dbKey: String, entryIds: List<String>) {
        step("removeEntries", dbKey)
        refuseOutsideBin(dbKey, entryIds)
        entryIds.distinct().forEach { removeEntry(dbKey, it) }
    }

    override suspend fun emptyTrash(dbKey: String) {
        step("emptyTrash", dbKey)
        val tree = tree(dbKey)
        val binId = tree.recycleBinId ?: return
        val bin = tree.groups.getValue(binId)
        bin.groupIds.forEach { removeGroups(dbKey, subtree(groupTrees.getValue(dbKey), it)) }
        bin.entryIds.forEach { removeEntry(dbKey, it) }
        val emptied = groupTrees.getValue(dbKey)
        groupTrees[dbKey] = emptied.copy(
            groups = emptied.groups + (binId to emptied.groups.getValue(binId).copy(groupIds = emptyList(), entryIds = emptyList())),
        )
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

    override suspend fun setFileChecksum(dbKey: String, bytes: ByteArray) {
        step("setFileChecksum", dbKey)
        checksumBytes[dbKey] = bytes.copyOf()
        // The file is what was written last - no longer somebody else's
        changedElsewhere -= dbKey
    }

    override suspend fun mergeDatabase(dbKey: String, bytes: ByteArray): MergeSummary {
        step("mergeDatabase", dbKey)
        bytesReceived = bytes.copyOf()
        return mergeSummary
    }

    override suspend fun mergeCopy(
        dbKey: String,
        bytes: ByteArray,
        password: String?,
        keyFile: KeyFile?,
    ): MergeSummary = mergeOther("mergeCopy", dbKey, bytes, password, keyFile)

    override suspend fun importDatabase(
        dbKey: String,
        bytes: ByteArray,
        password: String?,
        keyFile: KeyFile?,
    ): MergeSummary = mergeOther("importDatabase", dbKey, bytes, password, keyFile)

    /** The bytes of the copy's file [mergeCopyBothWays] hands back. */
    var mergedCopyBytes: ByteArray = byteArrayOf(9, 9, 9)

    override suspend fun mergeCopyBothWays(
        dbKey: String,
        bytes: ByteArray,
        password: String?,
        keyFile: KeyFile?,
    ): MergedBothWays =
        MergedBothWays(mergeOther("mergeCopyBothWays", dbKey, bytes, password, keyFile), mergedCopyBytes.copyOf())

    private fun mergeOther(
        method: String,
        dbKey: String,
        bytes: ByteArray,
        password: String?,
        keyFile: KeyFile?,
    ): MergeSummary {
        bytesReceived = bytes.copyOf()
        passwordReceived = password
        keyFile?.let { keyFilesReceived += it }
        step(method, dbKey)
        if (dbKey !in opened) throw CoreException("DbKeyNotFound", "not open: $dbKey")
        if (dbKey in locked) throw CoreException("DbLocked", "locked: $dbKey")
        return mergeSummary
    }

    /** The options the last password was generated with; null - none was. */
    var passwordOptions: PasswordOptions? = null
        private set

    override suspend fun generatePassword(options: PasswordOptions): String {
        step("generatePassword", "")
        passwordOptions = options
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

    // As the core: the entries of the subtree go to the bin flat - or for good with the bin off - the groups go for good
    override suspend fun deleteGroup(dbKey: String, groupId: String) {
        step("deleteGroup", dbKey)
        val group = tree(dbKey).groups[groupId] ?: throw notFound(groupId)
        checkNotNull(group.parentId) { "the root cannot be deleted" }
        val subtree = subtree(tree(dbKey), groupId)
        val entryIds = subtree.flatMap { tree(dbKey).groups.getValue(it).entryIds }
        if (recycleBinEnabled) {
            // By the tree: a test may name entries in it that it never stored
            val binId = recycleBin(dbKey)
            val tree = tree(dbKey)
            val bin = tree.groups.getValue(binId)
            groupTrees[dbKey] = tree.copy(
                groups = tree.groups + subtree.associateWith { tree.groups.getValue(it).copy(entryIds = emptyList()) } +
                    (binId to bin.copy(entryIds = bin.entryIds + entryIds)),
            )
            val entries = stored(dbKey)
            entryIds.forEach { id ->
                entries[id]?.let { entries[id] = it.copy(groupId = binId, previousGroupId = it.groupId, deletedAt = ++clock) }
            }
        }
        removeGroups(dbKey, subtree)
    }

    private fun subtree(tree: GroupTree, groupId: String): List<String> =
        listOf(groupId) + tree.groups.getValue(groupId).groupIds.flatMap { subtree(tree, it) }

    // The groups and the entries left in them go for good; the parent of the first one forgets it
    private fun removeGroups(dbKey: String, subtree: List<String>) {
        val tree = tree(dbKey)
        subtree.flatMap { tree.groups.getValue(it).entryIds }.forEach { stored(dbKey).remove(it) }
        val parent = tree.groups.getValue(subtree.first()).parentId?.let { tree.groups.getValue(it) }
        groupTrees[dbKey] = tree.copy(
            groups = tree.groups - subtree.toSet() +
                listOfNotNull(parent?.let { it.id to it.copy(groupIds = it.groupIds - subtree.first()) }),
        )
    }

    /**
     * An entry the fake holds: the group it is in, its strings in order, when it was made and changed, and
     * its old versions, oldest first.
     */
    data class FakeEntry(
        val groupId: String,
        val fields: List<EntryFieldData>,
        val createdAt: Long,
        val modifiedAt: Long = createdAt,
        val history: List<FakeVersion> = emptyList(),
        /** `PreviousParentGroup`: the group before the last move, as the core sets it on every move */
        val previousGroupId: String? = null,
        /** When the entry went to the bin */
        val deletedAt: Long? = null,
        /** Its attachments, in order; their bytes - in [attachmentBytes] */
        val attachments: List<EntryAttachmentData> = emptyList(),
    )

    /** One old version of an entry: its strings and attachments, and when it was saved. */
    data class FakeVersion(
        val fields: List<EntryFieldData>,
        val savedAt: Long,
        val attachments: List<EntryAttachmentData> = emptyList(),
    )

    /**
     * The bytes of the attachments of every database, by id - as the core keeps them, apart from the entries. A
     * test may put bytes here and link them with [FakeEntry.attachments]. Ids of the files added - `attachment-1`, …
     */
    val attachmentBytes = mutableMapOf<String, ByteArray>()

    private var attachmentsAdded = 0

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
        val found = stored(dbKey)
            .filter { (_, entry) -> !inRecycleBin(dbKey, entry.groupId) }
            .filter { (_, entry) -> entry.matches(term) }
            .map { (id, entry) -> summary(id, entry) }
        return EntrySearchData(term, found)
    }

    private fun FakeEntry.matches(term: String): Boolean {
        val needle = term.trim().lowercase()
        if (needle.isEmpty()) return false
        fun open(key: String) = fields.firstOrNull { it.key == key }?.takeUnless { it.protected }?.value.orEmpty()
        return listOf("Title", "UserName").any { open(it).lowercase().contains(needle) }
    }

    private fun trashed(dbKey: String): List<Pair<String, FakeEntry>> =
        stored(dbKey).filter { (_, entry) -> inRecycleBin(dbKey, entry.groupId) }.toList()

    private fun trashEntry(dbKey: String, id: String, entry: FakeEntry): TrashEntryData {
        val previous = entry.previousGroupId?.takeIf { it in tree(dbKey).groups && !inRecycleBin(dbKey, it) }
        return TrashEntryData(summary(id, entry), previous, entry.deletedAt ?: 0)
    }

    private fun refuseOutsideBin(dbKey: String, entryIds: List<String>) {
        entryIds.forEach { id ->
            if (!inRecycleBin(dbKey, found(dbKey, id).groupId)) throw CoreException("DataError", "$id is not in the bin")
        }
    }

    private fun inRecycleBin(dbKey: String, groupId: String): Boolean {
        val tree = tree(dbKey)
        val bin = tree.recycleBinId ?: return false
        return generateSequence(groupId) { tree.groups[it]?.parentId }.any { it == bin }
    }

    override suspend fun entry(dbKey: String, entryId: String): EntryData {
        step("entry", dbKey)
        val entry = found(dbKey, entryId)
        return EntryData(entry.groupId, entry.fields, entry.createdAt, entry.modifiedAt, entry.attachments)
    }

    override suspend fun createEntry(
        dbKey: String,
        groupId: String,
        fields: List<EntryFieldData>,
        newAttachments: List<NewAttachmentData>,
    ): String {
        step("createEntry", dbKey)
        tree(dbKey).groups[groupId] ?: throw notFound(groupId)
        refuseDuplicateKeys(fields)
        refuseAttachmentNames(newAttachments.map { it.name })
        val id = addEntry(dbKey, groupId, "entry-${++entriesCreated}", fields)
        stored(dbKey)[id] = found(dbKey, id).copy(attachments = newAttachments.map(::keep))
        return id
    }

    override suspend fun updateEntry(
        dbKey: String,
        entryId: String,
        fields: List<EntryFieldData>,
        attachments: EntryAttachmentsEdit?,
    ) {
        step("updateEntry", dbKey)
        val entry = found(dbKey, entryId)
        refuseDuplicateKeys(fields)
        val after = attachments?.let { edit ->
            // As the core: every check before anything is kept
            val kept = edit.keptNames.map { name ->
                entry.attachments.firstOrNull { it.name == name } ?: throw CoreException("DataError", "no attachment $name")
            }
            refuseAttachmentNames(kept.map { it.name } + edit.added.map { it.name })
            kept + edit.added.map(::keep)
        } ?: entry.attachments
        stored(dbKey)[entryId] = entry.updatedTo(fields, after)
    }

    override suspend fun attachmentBytes(dbKey: String, attachmentId: String): ByteArray {
        step("attachmentBytes", dbKey)
        return attachmentBytes[attachmentId]?.copyOf() ?: throw CoreException("NotFound", "no attachment $attachmentId")
    }

    // A copy of the bytes, as the core makes its own: the caller wipes what it handed over
    private fun keep(file: NewAttachmentData): EntryAttachmentData {
        val id = "attachment-${++attachmentsAdded}"
        attachmentBytes[id] = file.bytes.copyOf()
        return EntryAttachmentData(id = id, name = file.name, size = file.bytes.size.toLong())
    }

    private fun refuseAttachmentNames(names: List<String>) {
        if (names.any { it.isEmpty() }) throw CoreException("DataError", "an attachment has no name")
        if (names.size != names.toSet().size) throw CoreException("DataError", "two attachments with one name")
    }

    // As the core: into the bin, or for good with the bin off
    override suspend fun deleteEntry(dbKey: String, entryId: String) {
        step("deleteEntry", dbKey)
        delete(dbKey, entryId)
    }

    private fun delete(dbKey: String, entryId: String) {
        found(dbKey, entryId)
        if (!recycleBinEnabled) {
            removeEntry(dbKey, entryId)
            return
        }
        relocate(dbKey, entryId, recycleBin(dbKey))
        stored(dbKey)[entryId] = found(dbKey, entryId).copy(deletedAt = ++clock)
    }

    private fun removeEntry(dbKey: String, entryId: String) {
        val entry = found(dbKey, entryId)
        val tree = tree(dbKey)
        val parent = tree.groups.getValue(entry.groupId)
        groupTrees[dbKey] = tree.copy(groups = tree.groups + (parent.id to parent.copy(entryIds = parent.entryIds - entryId)))
        stored(dbKey).remove(entryId)
    }

    override suspend fun moveEntry(dbKey: String, entryId: String, groupId: String) {
        step("moveEntry", dbKey)
        relocate(dbKey, entryId, groupId)
    }

    private fun relocate(dbKey: String, entryId: String, groupId: String) {
        val entry = found(dbKey, entryId)
        val tree = tree(dbKey)
        val target = tree.groups[groupId] ?: throw notFound(groupId)
        val oldParent = tree.groups.getValue(entry.groupId)

        groupTrees[dbKey] = tree.copy(
            groups = tree.groups +
                (oldParent.id to oldParent.copy(entryIds = oldParent.entryIds - entryId)) +
                (groupId to target.copy(entryIds = target.entryIds + entryId)),
        )
        stored(dbKey)[entryId] = entry.copy(groupId = groupId, previousGroupId = entry.groupId)
    }

    override suspend fun entryHistory(dbKey: String, entryId: String): List<HistoryVersionData> {
        step("entryHistory", dbKey)
        return found(dbKey, entryId).history.mapIndexed { index, version -> HistoryVersionData(index, version.savedAt) }
    }

    override suspend fun historyVersion(dbKey: String, entryId: String, index: Int): EntryData {
        step("historyVersion", dbKey)
        val entry = found(dbKey, entryId)
        val version = entry.history.getOrNull(index) ?: throw CoreException("NotFound", "no version $index")
        return EntryData(entry.groupId, version.fields, entry.createdAt, version.savedAt, version.attachments)
    }

    override suspend fun restoreHistoryVersion(dbKey: String, entryId: String, index: Int) {
        step("restoreHistoryVersion", dbKey)
        val entry = found(dbKey, entryId)
        val version = entry.history.getOrNull(index) ?: throw CoreException("NotFound", "no version $index")
        stored(dbKey)[entryId] = entry.updatedTo(version.fields, version.attachments)
    }

    override suspend fun deleteHistoryVersion(dbKey: String, entryId: String, index: Int) {
        step("deleteHistoryVersion", dbKey)
        val entry = found(dbKey, entryId)
        if (index !in entry.history.indices) throw CoreException("NotFound", "no version $index")
        stored(dbKey)[entryId] = entry.copy(history = entry.history.filterIndexed { i, _ -> i != index })
    }

    override suspend fun deleteHistory(dbKey: String, entryId: String) {
        step("deleteHistory", dbKey)
        stored(dbKey)[entryId] = found(dbKey, entryId).copy(history = emptyList())
    }

    // Like the core: the state before goes into the history, and the change is stamped
    private fun FakeEntry.updatedTo(fields: List<EntryFieldData>, attachments: List<EntryAttachmentData>) =
        copy(
            fields = fields,
            attachments = attachments,
            modifiedAt = ++clock,
            history = history + FakeVersion(this.fields, modifiedAt, this.attachments),
        )

    private fun stored(dbKey: String): MutableMap<String, FakeEntry> {
        tree(dbKey)
        return storedEntries.getOrPut(dbKey) { mutableMapOf() }
    }

    private fun found(dbKey: String, entryId: String): FakeEntry =
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
