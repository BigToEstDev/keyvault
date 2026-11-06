package ru.kino.dev.core

import kotlin.time.Duration
import ru.kino.dev.core.preset.StandardPreset

/**
 * The kdbx core, as the rest of the app sees it.
 *
 * The implementation is native (Rust behind jni) and lives in the data layer; nothing above it needs to
 * know that. Calls suspend because the core is synchronous and some of its work is genuinely slow -
 * opening a database runs Argon2 - so the data layer moves it off the main thread.
 *
 * Step 22 declares only what the end to end check needs. The v1 set of operations is Step 23.
 */
interface NativeCore {

    /** Version of the native bridge and the time it was built, for the log on start. */
    suspend fun buildInfo(): String

    /**
     * Creates a new database and returns it together with the bytes to write into the chosen file.
     *
     * The core does not touch the file: the app owns it through SAF, so the contents come back here and
     * are written by the caller. [NewDatabase.dbKey] is the uri of that file and identifies the database
     * in every later call.
     */
    suspend fun createDatabase(database: NewDatabase): CreatedDatabase

    /**
     * Opens a database from the bytes of its file. The database stays open in the native library until
     * [closeDatabase], and every later call refers to it by [dbKey].
     *
     * Reading the file is the caller's: this interface is about the core, not about storage, and on
     * Android the bytes come from SAF.
     *
     * @param dbKey uri of the file, the identity of the database
     * @param bytes contents of the file
     * @param fileName name to show; passed in because a SAF uri holds no readable name
     * @param keyFile the key file, when the database needs one - read by the caller, like the database
     */
    suspend fun openDatabase(
        dbKey: String,
        bytes: ByteArray,
        password: String?,
        fileName: String?,
        keyFile: KeyFile? = null,
    ): OpenedDatabase

    /**
     * Serialises the open database and returns the bytes to write into its file.
     *
     * Writing is the caller's: the stream has to be opened truncating (`"wt"`), because the core does not
     * cut a writer short and a database that got smaller would keep the tail of the older one.
     */
    suspend fun saveDatabase(dbKey: String): ByteArray

    /**
     * Locks the open database: its content is encrypted in place, only ciphertext stays in memory. Unsaved
     * edits survive - they are encrypted, not dropped - so this is not a save and does not need one.
     *
     * The key stays with the core under [dbKey], which is what makes [unlockDatabase] quick.
     */
    suspend fun lockDatabase(dbKey: String)

    /**
     * Unlocks a database that was locked while open: the credentials are checked against the key the core
     * kept, and the file is not read again - which is why this is quick and opening is not.
     *
     * Only for a database the core still holds under [dbKey]. After the process died there is nothing to
     * unlock, and the database is opened anew with [openDatabase].
     *
     * @param keyFile the key file, when the database has one - read by the caller, as for opening
     */
    suspend fun unlockDatabase(dbKey: String, password: String?, keyFile: KeyFile? = null): OpenedDatabase

    /** Forgets the database: its contents and its key leave the process. */
    suspend fun closeDatabase(dbKey: String)

    /**
     * Whether [password] is the password of the open database - checked alone against what the core kept,
     * the key file not asked for, so it answers at once; locked or not, nothing changes. What the app asks
     * before it keeps the password for biometrics (pass-docs, Step 60). A database without a password
     * matches none.
     */
    suspend fun verifyPassword(dbKey: String, password: String): Boolean

    /** What the key of the open database is made of - a database without a password gets no biometrics. */
    suspend fun credentialsUsed(dbKey: String): CredentialsUsed

    /** The name of the open database, the version of KDBX its file was read as and its compression. */
    suspend fun databaseFile(dbKey: String): DatabaseFile

    /**
     * Gives the open database [name] - `Meta/DatabaseName`, what KeePass and KeePassXC show; not the root group,
     * not the file - and packs its content with [compression] from the next save on. The rest of its settings
     * stay as they are. In memory only: the file gets it on the next [saveDatabase].
     */
    suspend fun changeDatabaseFile(dbKey: String, name: String, compression: Compression)

    /** Which standard fields of the open database are private and how long the history of its entries is kept. */
    suspend fun databaseEntries(dbKey: String): DatabaseEntries

    /**
     * Sets the private fields and the history limits of the open database; the rest of its settings stay as they
     * are. A lower limit trims the history of every entry at once - there is no undo. In memory only: the file gets
     * it on the next [saveDatabase].
     */
    suspend fun changeDatabaseEntries(dbKey: String, entries: DatabaseEntries)

    /** The cipher, the key derivation and the credentials of the open database. */
    suspend fun databaseSecurity(dbKey: String): DatabaseSecurity

    /**
     * Encrypts the open database with [encryption] from the next save on; memory, lanes and the rest of its settings
     * stay as they are. In memory only: the file gets it on the next [saveDatabase].
     */
    suspend fun changeDatabaseEncryption(dbKey: String, encryption: Encryption)

    /**
     * Applies [change] to the credentials of the open database. Both password and key file gone is refused by the
     * core (`InSufficientCredentials`), a password over [CredentialLimits.PASSWORD_MAX_CHARS] too - before anything
     * changes. In memory only: the old file opens with the old credentials until the next [saveDatabase].
     */
    suspend fun changeDatabaseCredentials(dbKey: String, change: CredentialsChange)

    /**
     * How long one KDF pass of the open database takes on this device - the open time is one pass times the
     * iterations. Takes about as long as a pass; the database is not changed, locked or not (pass-docs, Step 68).
     */
    suspend fun measureKdfPass(dbKey: String): Duration

    /** Whether the open database uses the recycle bin - `Meta/RecycleBinEnabled`; a new database does. */
    suspend fun recycleBinEnabled(dbKey: String): Boolean

    /**
     * Switches the recycle bin of the open database on or off; the rest of its settings stay as they are. Off, a
     * delete is for good, and the bin goes at once with all it holds - there is no undo. On again, the first delete
     * makes a new bin. In memory only: the file gets it on the next [saveDatabase].
     */
    suspend fun changeRecycleBinEnabled(dbKey: String, enabled: Boolean)

    /** The entries of the recycle bin, from any depth of it, in no particular order (pass-docs, Step 69). */
    suspend fun trashEntries(dbKey: String): List<TrashEntryData>

    /** The entries of the recycle bin found by [term] - the same match as [searchEntries]. */
    suspend fun searchTrash(dbKey: String, term: String): TrashSearchData

    /**
     * Moves [entryIds] out of the recycle bin into [groupId] - a living group, not the bin or in it. All or nothing:
     * one entry outside the bin, or such a group, and nothing moves. In memory only.
     */
    suspend fun restoreEntries(dbKey: String, entryIds: List<String>, groupId: String)

    /** Removes [entryIds] of the recycle bin for good, all or nothing. In memory only. */
    suspend fun removeEntries(dbKey: String, entryIds: List<String>)

    /** Removes everything in the recycle bin for good; the bin stays. In memory only. */
    suspend fun emptyTrash(dbKey: String)

    /**
     * Compares [bytes] with the checksum taken when the file was last read or written.
     *
     * Throws [CoreException] with kind `DbFileContentChangeDetected` when they differ - somebody else
     * wrote the file, and that is an ordinary outcome the ui answers with a merge, not a breakage.
     */
    suspend fun verifyFileChecksum(dbKey: String, bytes: ByteArray)

    /**
     * Merges the database in [bytes] into the open one and reports what changed.
     *
     * Works on the stored composite key, so no password is asked for - which is also why a file
     * re-encrypted elsewhere comes back as `MergeFailedCredentialsChanged` rather than as a wrong
     * password. There is no undo: the database in memory changes at once, and the file is only touched
     * when the caller saves afterwards.
     */
    suspend fun mergeDatabase(dbKey: String, bytes: ByteArray): MergeSummary

    /**
     * Merges another copy of the open database - the file from another device - and reports what changed
     * (pass-docs, Step 106).
     *
     * The copy is opened with its own [password] and [keyFile]: they may differ from the open database's, and
     * the merged database keeps the open one's. Changes are by the rules of KeePass: the newer edit wins, the
     * histories are joined, a deletion wins over an older edit. In memory only - the caller saves; the
     * checksum of the open file stays, the bytes are another file.
     *
     * Throws [CoreException] with kind `NotSameDatabase` for a different database - nothing changed, that is
     * [importDatabase]; `InvalidCredentials` for wrong credentials of the copy; `DbLocked` for a locked database.
     */
    suspend fun mergeCopy(
        dbKey: String,
        bytes: ByteArray,
        password: String?,
        keyFile: KeyFile? = null,
    ): MergeSummary

    /**
     * Brings a different database into the open one, whole - the same as [mergeCopy] otherwise.
     *
     * Throws [CoreException] with kind `SameDatabase` for a copy of the open database - nothing changed, that
     * is [mergeCopy].
     */
    suspend fun importDatabase(
        dbKey: String,
        bytes: ByteArray,
        password: String?,
        keyFile: KeyFile? = null,
    ): MergeSummary

    /** Generates a password with the core's generator. */
    suspend fun generatePassword(options: PasswordOptions = PasswordOptions()): String

    /**
     * Generates a new key file - 32 random bytes as xml KeyFile 2.0 - and returns its content.
     *
     * Writing it is the caller's, through SAF "save as", and so is refusing to write over an existing
     * file: it may be the key of another database, and replacing it locks that database for good.
     */
    suspend fun generateKeyFile(): ByteArray

    /** The whole tree of groups in one answer - the recycle bin and what is inside it included. */
    suspend fun groupTree(dbKey: String): GroupTree

    /**
     * Creates a group named [name] inside [parentId] and returns its id. Every other field is the core's
     * default for a blank group.
     *
     * In memory only, like every change of the database: the file gets it on the next [saveDatabase].
     */
    suspend fun createGroup(dbKey: String, parentId: String, name: String): String

    /**
     * Renames the group. The rest of it - notes, icon, the fields of another program the core keeps - goes
     * back as it was read. In memory only.
     */
    suspend fun renameGroup(dbKey: String, groupId: String, name: String)

    /**
     * Deletes the group with its subtree: the entries go to the recycle bin, flat - the core creates the bin
     * when the database has none - and the groups go for good; only entries are kept in the bin. Without a
     * bin (`RecycleBinEnabled` off) everything goes for good. In memory only.
     */
    suspend fun deleteGroup(dbKey: String, groupId: String)

    /**
     * The entries directly inside [groupId], not those of the groups below, in the order the core gives
     * them - by title. A group that is not there has no entries.
     */
    suspend fun entries(dbKey: String, groupId: String): List<EntrySummaryData>

    /**
     * The entries of the whole database found by [term], in no particular order. An entry is found by its
     * title or its user name - as its card shows them - holding the term as plain text, case aside; a
     * protected field is never looked into, and the recycle bin, with the groups inside it, is not looked
     * at. A blank term finds nothing.
     */
    suspend fun searchEntries(dbKey: String, term: String): EntrySearchData

    /** The entry: the group it is directly in, its strings in its order, and its attachments. */
    suspend fun entry(dbKey: String, entryId: String): EntryData

    /**
     * Creates an entry inside [groupId] holding exactly [fields], in that order, with [newAttachments] - in one
     * call, and returns its id. The rest of it - uuid, times, icon - is the core's default for a new entry. Two
     * fields under one key are refused with kind `DuplicateFieldKey`; two attachments under one name, or one
     * with no name, with kind `DataError` - nothing is created then. The size of a file is the app's to check
     * before. In memory only.
     */
    suspend fun createEntry(
        dbKey: String,
        groupId: String,
        fields: List<EntryFieldData>,
        newAttachments: List<NewAttachmentData> = emptyList(),
    ): String

    /**
     * Replaces the strings of the entry with [fields], in that order, and - when [attachments] is given - its
     * attachments with those kept and added, in one call; null keeps them as they are. The rest of it - icon,
     * tags, times, the fields of another program the core keeps - goes back as it was read; the core puts the
     * previous state into the history. Two fields under one key are refused with kind `DuplicateFieldKey`; two
     * attachments under one name, one with no name, or a kept name the entry has not, with kind `DataError` -
     * the entry stays as it was. In memory only.
     */
    suspend fun updateEntry(
        dbKey: String,
        entryId: String,
        fields: List<EntryFieldData>,
        attachments: EntryAttachmentsEdit? = null,
    )

    /**
     * The bytes of the attachment [attachmentId] of the open database - for "Download"; of an entry or of one of
     * its versions alike. An id the database has not is refused with kind `NotFound`. The bytes are the
     * caller's to wipe once written to the file.
     */
    suspend fun attachmentBytes(dbKey: String, attachmentId: String): ByteArray

    /**
     * Deletes the entry: into the recycle bin - the core creates the bin when the database has none - or for
     * good without a bin (`RecycleBinEnabled` off). In memory only.
     */
    suspend fun deleteEntry(dbKey: String, entryId: String)

    /** Moves the entry into the group [groupId], with nothing else of it changed. In memory only. */
    suspend fun moveEntry(dbKey: String, entryId: String, groupId: String)

    /**
     * The old versions of the entry, oldest first, as the core keeps them; the current state is not among
     * them. An entry that is not there is refused with kind `NotFound`.
     */
    suspend fun entryHistory(dbKey: String, entryId: String): List<HistoryVersionData>

    /**
     * The old version at [index] as an entry: its strings, and [EntryData.modifiedAt] - when it was saved; the
     * group is the entry's now - a version does not keep one. A version or an entry that is not there is
     * refused with kind `NotFound`.
     */
    suspend fun historyVersion(dbKey: String, entryId: String, index: Int): EntryData

    /**
     * Makes the old version at [index] the entry again - whole, as it was: its strings, icon, tags,
     * attachments; the group stays the entry's. The core puts the current state into the history, as on any
     * update. A version or an entry that is not there is refused with kind `NotFound`. In memory only.
     */
    suspend fun restoreHistoryVersion(dbKey: String, entryId: String, index: Int)

    /**
     * Deletes the old version at [index]; the versions after it move up by one. A version or an entry that
     * is not there is refused with kind `NotFound`. In memory only.
     */
    suspend fun deleteHistoryVersion(dbKey: String, entryId: String, index: Int)

    /**
     * Deletes every old version of the entry; the entry itself and when it was made stay. An entry that is
     * not there is refused with kind `NotFound`. In memory only.
     */
    suspend fun deleteHistory(dbKey: String, entryId: String)
}

/**
 * A database to create.
 *
 * Defaults are the core's: Argon2id and AES-256, which is what a new KeePass database is expected to be.
 * The kdf parameters are deliberately not here - they belong to a decision about how long unlocking may
 * take on a phone, not to a call site.
 */
data class NewDatabase(
    /** Uri of the file the database will live in; also its identity in the core */
    val dbKey: String,
    /** Name to show, taken from the document rather than from the uri */
    val fileName: String?,
    val databaseName: String,
    val databaseDescription: String? = null,
    /** At most [CredentialLimits.PASSWORD_MAX_CHARS] characters - longer is refused by the core */
    val password: String?,
    /** The key file, when the database is to have one */
    val keyFile: KeyFile? = null,
)

/** A database the core has just created, and the bytes to write into its file. */
data class CreatedDatabase(
    val database: OpenedDatabase,
    val bytes: ByteArray,
) {
    // ByteArray in a data class compares by identity, which is never what a caller means
    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is CreatedDatabase && database == other.database && bytes.contentEquals(other.bytes))

    override fun hashCode(): Int = 31 * database.hashCode() + bytes.contentHashCode()
}

/** What the key of an open database is made of: a password, a key file or both (pass-docs, Step 60). */
data class CredentialsUsed(val password: Boolean, val keyFile: Boolean)

/**
 * The file of an open database as its settings show it (pass-docs, Step 63).
 *
 * @param name the name of the database inside the file, `Meta/DatabaseName`
 * @param version the version of KDBX the file was read as; 4.1 once saved - the core writes no other
 */
data class DatabaseFile(val name: String, val version: KdbxVersion, val compression: Compression)

/** The entries of an open database as its settings show them (pass-docs, Step 65). */
data class DatabaseEntries(val protection: FieldProtection, val history: HistoryLimits)

/**
 * Which standard fields of an entry are private - `Meta/MemoryProtection`, the same in KeePass and KeePassXC. The
 * core keeps it, the app applies it: a field it names is hidden on every entry, whatever the preset says.
 */
data class FieldProtection(
    val title: Boolean,
    val userName: Boolean,
    val password: Boolean,
    val url: Boolean,
    val notes: Boolean,
) {

    /** Whether the field of [key] is private by the database; a field of one's own never is. */
    fun protects(key: String): Boolean = when (key) {
        StandardPreset.TITLE -> title
        StandardPreset.USER_NAME -> userName
        StandardPreset.PASSWORD -> password
        StandardPreset.URL -> url
        StandardPreset.NOTES -> notes
        else -> false
    }

    companion object {
        /** What a new database has, as KeePass and KeePassXC: only the password. */
        val DEFAULT = FieldProtection(title = false, userName = false, password = true, url = false, notes = false)
    }
}

/**
 * How much history every entry keeps; null - no limit.
 *
 * @param maxItems the versions kept
 * @param maxSize the bytes the history of an entry takes at most
 */
data class HistoryLimits(val maxItems: Int?, val maxSize: Long?)

/** A version of KDBX the core reads. */
enum class KdbxVersion { V40, V41 }

/** How the content of a file is packed before it is encrypted. GZip is what a new database and KeePass write. */
enum class Compression { None, GZip }

/** An open database, as the core describes it. */
data class OpenedDatabase(
    val dbKey: String,
    val databaseName: String,
    /**
     * Name of the file. Unreliable after unlocking: the core derives it from [dbKey] there, and a SAF uri
     * has no readable name in it - so the app keeps its own and ignores this one. See the note in
     * pass-docs, plan/todo/core/rust-core-bugs.md.
     */
    val fileName: String?,
    /** Name of the key file it was opened with, as it was passed in - to show, never to open */
    val keyFileName: String?,
)

/**
 * What a merge changed, as counts.
 *
 * The core also names every group and entry it touched. The names are left there on purpose: a summary is
 * what a screen shows, and carrying the contents of a database around in a result object is how they end
 * up in a log. A screen that lists the changes asks for them when it exists.
 *
 * After [NativeCore.mergeDatabase] [differentDatabases] is only known once the merge has happened; a merge of
 * a copy and an import check it before anything changes (pass-docs, Step 106).
 */
data class MergeSummary(
    val addedGroups: Int,
    val updatedGroups: Int,
    val movedGroups: Int,
    val addedEntries: Int,
    val updatedEntries: Int,
    val movedEntries: Int,
    val deletedGroups: Int,
    val deletedEntries: Int,
    val metaDataChanged: Boolean,
    val mergeDone: Boolean,
    val differentDatabases: Boolean,
)

/**
 * Options of the password generator. The defaults are a plain password of this app, not the core's own (8
 * characters, no upper case there); what the form generates is `vault`'s (pass-docs, Step 104).
 */
data class PasswordOptions(
    val length: Int = 16,
    val numbers: Boolean = true,
    val lowercaseLetters: Boolean = true,
    val uppercaseLetters: Boolean = true,
    val symbols: Boolean = false,
    val spaces: Boolean = false,
    val excludeSimilarCharacters: Boolean = true,
    /** When true every requested character class must appear in the result. */
    val strict: Boolean = true,
)

/**
 * A failure reported by the core or by the bridge.
 *
 * [kind] is the name of the core's error variant (`NotFound`, `DbKeyNotFound`, …) or one of the bridge's
 * own (`UnknownCommand`, `InvalidArguments`). It is what the ui should branch on; [message] is for the
 * log and for the rare case where there is nothing better to show.
 *
 * This is deliberately an exception and not a sealed result type: almost every caller wants to let the
 * failure travel up to the screen that can show it, and a `Result` at every level would be noise. The
 * screens that do handle a specific failure match on [kind].
 */
class CoreException(
    val kind: String,
    override val message: String,
) : Exception("$kind: $message")
