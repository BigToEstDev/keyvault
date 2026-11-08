package ru.kino.dev.ffi

import java.util.Base64
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import ru.kino.dev.core.Compression
import ru.kino.dev.core.CreatedDatabase
import ru.kino.dev.core.CredentialLimits
import ru.kino.dev.core.CredentialsChange
import ru.kino.dev.core.CredentialsUsed
import ru.kino.dev.core.DatabaseEntries
import ru.kino.dev.core.DatabaseFile
import ru.kino.dev.core.DatabaseSecurity
import ru.kino.dev.core.Encryption
import ru.kino.dev.core.EntryAttachmentsEdit
import ru.kino.dev.core.EntryData
import ru.kino.dev.core.EntryFieldData
import ru.kino.dev.core.EntrySearchData
import ru.kino.dev.core.EntrySummaryData
import ru.kino.dev.core.FieldProtection
import ru.kino.dev.core.GroupTree
import ru.kino.dev.core.HistoryLimits
import ru.kino.dev.core.HistoryVersionData
import ru.kino.dev.core.KdbxVersion
import ru.kino.dev.core.KeyFile
import ru.kino.dev.core.MergeSummary
import ru.kino.dev.core.MergedBothWays
import ru.kino.dev.core.NativeCore
import ru.kino.dev.core.NewAttachmentData
import ru.kino.dev.core.NewDatabase
import ru.kino.dev.core.OpenedDatabase
import ru.kino.dev.core.PasswordOptions
import ru.kino.dev.core.TrashEntryData
import ru.kino.dev.core.TrashSearchData

/**
 * [NativeCore] on top of the jni bridge.
 *
 * The native side is synchronous by design - there is no async across the jni boundary and no callbacks
 * back into Kotlin - so this is where the work leaves the caller's thread. Argon2 and file io block, and
 * they block a dispatcher thread instead of the main one.
 */
internal class NativeCoreFfi : NativeCore {

    // Fixed, not a parameter: a qualified dispatcher provider is worth adding once something else needs one too
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO

    override suspend fun buildInfo(): String = withContext(dispatcher) {
        PassFfi.buildInfo()
    }

    override suspend fun createDatabase(database: NewDatabase): CreatedDatabase =
        withContext(dispatcher) {
            val answer = FfiBinaryCall.call(
                command = FfiCommands.CREATE_DATABASE,
                argsJson = encode(NewDatabaseDto.serializer(), database.toDto()),
            )
            val loaded = FfiEnvelope.unwrap(answer.envelope, KdbxLoadedDto.serializer())

            CreatedDatabase(database = loaded.toDomain(), bytes = answer.payload)
        }

    override suspend fun openDatabase(
        dbKey: String,
        bytes: ByteArray,
        password: String?,
        fileName: String?,
        keyFile: KeyFile?,
    ): OpenedDatabase = withContext(dispatcher) {
        val args = ReadKdbxDto(
            dbKey = dbKey,
            password = password,
            keyFile = keyFile?.toDto(),
            fileName = fileName,
        )
        val answer = FfiBinaryCall.call(
            command = FfiCommands.READ_DATABASE,
            argsJson = encode(ReadKdbxDto.serializer(), args),
            input = bytes,
        )

        FfiEnvelope.unwrap(answer.envelope, KdbxLoadedDto.serializer()).toDomain()
    }

    override suspend fun saveDatabase(dbKey: String): ByteArray = withContext(dispatcher) {
        val answer = FfiBinaryCall.call(
            command = FfiCommands.SAVE_DATABASE,
            argsJson = encode(DbKeyDto.serializer(), DbKeyDto(dbKey)),
        )
        // The envelope is read first: a refusal has no bytes behind it, and it has to travel as the
        // failure it is rather than as an empty file
        FfiEnvelope.unwrap(answer.envelope, KdbxSavedDto.serializer())

        answer.payload
    }

    override suspend fun closeDatabase(dbKey: String) {
        withContext(dispatcher) {
            val envelope = PassFfi.invoke(
                FfiCommands.CLOSE_DATABASE,
                encode(DbKeyDto.serializer(), DbKeyDto(dbKey)),
            )
            FfiEnvelope.unwrap(envelope, DoneDto.serializer())
        }
    }

    override suspend fun lockDatabase(dbKey: String) {
        withContext(dispatcher) {
            val envelope = PassFfi.invoke(
                FfiCommands.LOCK_DATABASE,
                encode(DbKeyDto.serializer(), DbKeyDto(dbKey)),
            )
            FfiEnvelope.unwrap(envelope, DoneDto.serializer())
        }
    }

    override suspend fun unlockDatabase(
        dbKey: String,
        password: String?,
        keyFile: KeyFile?,
    ): OpenedDatabase = withContext(dispatcher) {
        val envelope = PassFfi.invoke(
            FfiCommands.UNLOCK_DATABASE,
            encode(
                UnlockDto.serializer(),
                UnlockDto(dbKey = dbKey, password = password, keyFile = keyFile?.toDto()),
            ),
        )
        FfiEnvelope.unwrap(envelope, KdbxLoadedDto.serializer()).toDomain()
    }

    override suspend fun verifyPassword(dbKey: String, password: String): Boolean = withContext(dispatcher) {
        val envelope = PassFfi.invoke(
            FfiCommands.VERIFY_PASSWORD,
            encode(VerifyPasswordDto.serializer(), VerifyPasswordDto(dbKey = dbKey, password = password)),
        )
        FfiEnvelope.unwrap(envelope, MatchesDto.serializer()).matches
    }

    override suspend fun credentialsUsed(dbKey: String): CredentialsUsed = withContext(dispatcher) {
        val envelope = PassFfi.invoke(
            FfiCommands.CREDENTIALS_USED,
            encode(DbKeyDto.serializer(), DbKeyDto(dbKey)),
        )
        FfiEnvelope.unwrap(envelope, CredentialsUsedDto.serializer()).let {
            CredentialsUsed(password = it.password, keyFile = it.keyFile)
        }
    }

    override suspend fun databaseFile(dbKey: String): DatabaseFile = withContext(dispatcher) {
        val settings = dbSettings(dbKey)
        DatabaseFile(
            name = settings.meta.databaseName,
            version = when (settings.kdbxVersion) {
                "4.0" -> KdbxVersion.V40
                "4.1" -> KdbxVersion.V41
                else -> error("a version the core does not read: ${settings.kdbxVersion}")
            },
            compression = Compression.valueOf(settings.compression),
        )
    }

    override suspend fun changeDatabaseFile(dbKey: String, name: String, compression: Compression) {
        withContext(dispatcher) {
            // The whole settings go back, as the core gave them: only the name and the compression are ours
            val settings = dbSettings(dbKey)
            val changed = settings.copy(
                meta = settings.meta.copy(databaseName = name),
                compression = compression.name,
            )
            call(
                FfiCommands.SET_DB_SETTINGS,
                SetDbSettingsDto.serializer(),
                SetDbSettingsDto(dbKey = dbKey, settings = changed),
                DoneDto.serializer(),
            )
        }
    }

    override suspend fun databaseEntries(dbKey: String): DatabaseEntries = withContext(dispatcher) {
        val meta = dbSettings(dbKey).meta
        DatabaseEntries(
            protection = meta.memoryProtection.let {
                FieldProtection(title = it.title, userName = it.userName, password = it.password, url = it.url, notes = it.notes)
            },
            // Any negative is no limit, as KeePass reads it; -1 is what it writes
            history = HistoryLimits(
                maxItems = meta.historyMaxItems.takeIf { it >= 0 },
                maxSize = meta.historyMaxSize.takeIf { it >= 0 },
            ),
        )
    }

    override suspend fun changeDatabaseEntries(dbKey: String, entries: DatabaseEntries) {
        withContext(dispatcher) {
            // The whole settings go back, as the core gave them: only the protection and the limits are ours
            val settings = dbSettings(dbKey)
            val protection = entries.protection
            val changed = settings.copy(
                meta = settings.meta.copy(
                    memoryProtection = MemoryProtectionDto(
                        title = protection.title,
                        userName = protection.userName,
                        password = protection.password,
                        url = protection.url,
                        notes = protection.notes,
                    ),
                    historyMaxItems = entries.history.maxItems ?: NO_LIMIT.toInt(),
                    historyMaxSize = entries.history.maxSize ?: NO_LIMIT,
                ),
            )
            call(
                FfiCommands.SET_DB_SETTINGS,
                SetDbSettingsDto.serializer(),
                SetDbSettingsDto(dbKey = dbKey, settings = changed),
                DoneDto.serializer(),
            )
        }
    }

    override suspend fun databaseSecurity(dbKey: String): DatabaseSecurity = withContext(dispatcher) {
        dbSettings(dbKey).securityToDomain()
    }

    override suspend fun changeDatabaseEncryption(dbKey: String, encryption: Encryption) {
        withContext(dispatcher) {
            call(
                FfiCommands.SET_DB_SETTINGS,
                SetDbSettingsDto.serializer(),
                SetDbSettingsDto(dbKey = dbKey, settings = dbSettings(dbKey).withEncryption(encryption)),
                DoneDto.serializer(),
            )
        }
    }

    override suspend fun changeDatabaseCredentials(dbKey: String, change: CredentialsChange) {
        withContext(dispatcher) {
            // Only a new key file travels, as content, beside the settings - the core refuses it anywhere else
            val keyFile = (change as? CredentialsChange.SetKeyFile)?.keyFile?.toDto()
            call(
                FfiCommands.SET_DB_SETTINGS,
                SetDbSettingsDto.serializer(),
                SetDbSettingsDto(dbKey = dbKey, settings = dbSettings(dbKey).withCredentials(change), keyFile = keyFile),
                DoneDto.serializer(),
            )
        }
    }

    override suspend fun measureKdfPass(dbKey: String): Duration = withContext(dispatcher) {
        call(FfiCommands.MEASURE_KDF_PASS, DbKeyDto.serializer(), DbKeyDto(dbKey), KdfPassDto.serializer())
            .millis.milliseconds
    }

    override suspend fun recycleBinEnabled(dbKey: String): Boolean = withContext(dispatcher) {
        dbSettings(dbKey).meta.recycleBinEnabled
    }

    override suspend fun changeRecycleBinEnabled(dbKey: String, enabled: Boolean) {
        withContext(dispatcher) {
            // The whole settings go back, as the core gave them: only the flag is ours
            val settings = dbSettings(dbKey)
            call(
                FfiCommands.SET_DB_SETTINGS,
                SetDbSettingsDto.serializer(),
                SetDbSettingsDto(dbKey = dbKey, settings = settings.copy(meta = settings.meta.copy(recycleBinEnabled = enabled))),
                DoneDto.serializer(),
            )
        }
    }

    override suspend fun trashEntries(dbKey: String): List<TrashEntryData> = withContext(dispatcher) {
        call(
            FfiCommands.TRASH_ENTRIES,
            DbKeyDto.serializer(),
            DbKeyDto(dbKey),
            ListSerializer(TrashEntryDto.serializer()),
        ).map { it.toDomain() }
    }

    override suspend fun searchTrash(dbKey: String, term: String): TrashSearchData = withContext(dispatcher) {
        call(
            FfiCommands.SEARCH_TRASH,
            SearchTermDto.serializer(),
            SearchTermDto(dbKey = dbKey, term = term),
            TrashSearchResultDto.serializer(),
        ).toDomain()
    }

    override suspend fun restoreEntries(dbKey: String, entryIds: List<String>, groupId: String) {
        withContext(dispatcher) {
            call(
                FfiCommands.RESTORE_ENTRIES,
                RestoreEntriesDto.serializer(),
                RestoreEntriesDto(dbKey = dbKey, entryUuids = entryIds, groupUuid = groupId),
                DoneDto.serializer(),
            )
        }
    }

    override suspend fun removeEntries(dbKey: String, entryIds: List<String>) {
        withContext(dispatcher) {
            call(
                FfiCommands.REMOVE_ENTRIES_PERMANENTLY,
                EntryUuidsDto.serializer(),
                EntryUuidsDto(dbKey = dbKey, entryUuids = entryIds),
                DoneDto.serializer(),
            )
        }
    }

    override suspend fun emptyTrash(dbKey: String) {
        withContext(dispatcher) {
            call(FfiCommands.EMPTY_TRASH, DbKeyDto.serializer(), DbKeyDto(dbKey), DoneDto.serializer())
        }
    }

    private fun dbSettings(dbKey: String): DbSettingsDto =
        call(FfiCommands.GET_DB_SETTINGS, DbKeyDto.serializer(), DbKeyDto(dbKey), DbSettingsDto.serializer())

    override suspend fun verifyFileChecksum(dbKey: String, bytes: ByteArray) {
        withContext(dispatcher) {
            val answer = FfiBinaryCall.call(
                command = FfiCommands.VERIFY_DB_FILE_CHECKSUM,
                argsJson = encode(DbKeyDto.serializer(), DbKeyDto(dbKey)),
                input = bytes,
            )
            FfiEnvelope.unwrap(answer.envelope, DoneDto.serializer())
        }
    }

    override suspend fun setFileChecksum(dbKey: String, bytes: ByteArray) {
        withContext(dispatcher) {
            val answer = FfiBinaryCall.call(
                command = FfiCommands.SET_DB_FILE_CHECKSUM,
                argsJson = encode(DbKeyDto.serializer(), DbKeyDto(dbKey)),
                input = bytes,
            )
            FfiEnvelope.unwrap(answer.envelope, DoneDto.serializer())
        }
    }

    override suspend fun mergeDatabase(dbKey: String, bytes: ByteArray): MergeSummary =
        withContext(dispatcher) {
            val answer = FfiBinaryCall.call(
                command = FfiCommands.MERGE_DATABASE,
                argsJson = encode(DbKeyDto.serializer(), DbKeyDto(dbKey)),
                input = bytes,
            )
            FfiEnvelope.unwrap(answer.envelope, MergeResultDto.serializer()).toDomain()
        }

    override suspend fun mergeCopy(
        dbKey: String,
        bytes: ByteArray,
        password: String?,
        keyFile: KeyFile?,
    ): MergeSummary = mergeOther(FfiCommands.MERGE_COPY, dbKey, bytes, password, keyFile).summary

    override suspend fun importDatabase(
        dbKey: String,
        bytes: ByteArray,
        password: String?,
        keyFile: KeyFile?,
    ): MergeSummary = mergeOther(FfiCommands.IMPORT_DATABASE, dbKey, bytes, password, keyFile).summary

    override suspend fun mergeCopyBothWays(
        dbKey: String,
        bytes: ByteArray,
        password: String?,
        keyFile: KeyFile?,
    ): MergedBothWays = mergeOther(FfiCommands.MERGE_COPY_BOTH_WAYS, dbKey, bytes, password, keyFile)

    /** The merge [command] of another file; the bytes the core hands back - only both ways - come beside it. */
    private suspend fun mergeOther(
        command: String,
        dbKey: String,
        bytes: ByteArray,
        password: String?,
        keyFile: KeyFile?,
    ): MergedBothWays = withContext(dispatcher) {
        val args = OtherDatabaseDto(dbKey = dbKey, password = password, keyFile = keyFile?.toDto())
        val answer = FfiBinaryCall.call(
            command = command,
            argsJson = encode(OtherDatabaseDto.serializer(), args),
            input = bytes,
        )
        val summary = FfiEnvelope.unwrap(answer.envelope, MergeResultDto.serializer()).toDomain()
        MergedBothWays(summary, answer.payload)
    }

    override suspend fun generatePassword(options: PasswordOptions): String = withContext(dispatcher) {
        val args = FfiEnvelope.json.encodeToString(
            PasswordOptionsDto.serializer(),
            options.toDto(),
        )
        val envelope = PassFfi.invoke(FfiCommands.GENERATE_PASSWORD, args)
        FfiEnvelope.unwrap(envelope, GeneratedPasswordDto.serializer()).password
    }

    override suspend fun generateKeyFile(): ByteArray = withContext(dispatcher) {
        // No arguments; the key file comes back in the binary slot, the way a saved database does
        val answer = FfiBinaryCall.call(command = FfiCommands.GENERATE_KEY_FILE, argsJson = "")
        FfiEnvelope.unwrap(answer.envelope, DoneDto.serializer())

        answer.payload
    }

    override suspend fun groupTree(dbKey: String): GroupTree = withContext(dispatcher) {
        call(
            FfiCommands.GROUPS_SUMMARY_DATA,
            DbKeyDto.serializer(),
            DbKeyDto(dbKey),
            GroupTreeDto.serializer(),
        ).toDomain()
    }

    override suspend fun createGroup(dbKey: String, parentId: String, name: String): String =
        withContext(dispatcher) {
            // The core builds the blank group - uuid, times, defaults - so a new group is the same shape as
            // one read from a file, and only the name is ours
            val blank = call(
                FfiCommands.NEW_BLANK_GROUP_WITH_PARENT,
                NewBlankGroupWithParentDto.serializer(),
                NewBlankGroupWithParentDto(parentGroupUuid = parentId, markAsCategory = false),
                GroupDto.serializer(),
            )
            val group = blank.named(name)
            call(
                FfiCommands.INSERT_GROUP,
                GroupArgsDto.serializer(),
                GroupArgsDto(dbKey, group),
                DoneDto.serializer(),
            )

            group.uuid
        }

    override suspend fun renameGroup(dbKey: String, groupId: String, name: String) {
        withContext(dispatcher) {
            val group = call(
                FfiCommands.GET_GROUP_BY_ID,
                GroupIdDto.serializer(),
                GroupIdDto(dbKey = dbKey, groupUuid = groupId),
                GroupDto.serializer(),
            )
            call(
                FfiCommands.UPDATE_GROUP,
                GroupArgsDto.serializer(),
                GroupArgsDto(dbKey, group.named(name)),
                DoneDto.serializer(),
            )
        }
    }

    override suspend fun deleteGroup(dbKey: String, groupId: String) {
        withContext(dispatcher) {
            call(
                FfiCommands.DELETE_GROUP,
                GroupIdDto.serializer(),
                GroupIdDto(dbKey = dbKey, groupUuid = groupId),
                DoneDto.serializer(),
            )
        }
    }

    override suspend fun entries(dbKey: String, groupId: String): List<EntrySummaryData> =
        withContext(dispatcher) {
            call(
                FfiCommands.ENTRY_SUMMARY_DATA,
                EntrySummaryArgsDto.serializer(),
                entriesOfGroup(dbKey, groupId),
                ListSerializer(EntrySummaryDto.serializer()),
            ).map { it.toDomain() }
        }

    override suspend fun searchEntries(dbKey: String, term: String): EntrySearchData =
        withContext(dispatcher) {
            call(
                FfiCommands.SEARCH_TERM,
                SearchTermDto.serializer(),
                SearchTermDto(dbKey = dbKey, term = term),
                EntrySearchResultDto.serializer(),
            ).toDomain()
        }

    override suspend fun entry(dbKey: String, entryId: String): EntryData =
        withContext(dispatcher) {
            entryForm(dbKey, entryId).toEntryData()
        }

    override suspend fun createEntry(
        dbKey: String,
        groupId: String,
        fields: List<EntryFieldData>,
        newAttachments: List<NewAttachmentData>,
    ): String =
        withContext(dispatcher) {
            // The core builds the blank entry - uuid, times, icon - so a new entry is the same shape as one
            // read from a file, and only the strings are ours
            val blank = call(
                FfiCommands.NEW_ENTRY_FORM_DATA,
                NewEntryFormDto.serializer(),
                NewEntryFormDto(parentGroupUuid = groupId),
                EntryFormDataDto.serializer(),
            )
            val entry = blank.withFields(fields)
            saveForm(FfiCommands.INSERT_ENTRY_FROM_FORM_DATA, dbKey, entry, newAttachments)

            entry.uuid
        }

    override suspend fun updateEntry(
        dbKey: String,
        entryId: String,
        fields: List<EntryFieldData>,
        attachments: EntryAttachmentsEdit?,
    ) {
        withContext(dispatcher) {
            val read = entryForm(dbKey, entryId).withFields(fields)
            val entry = attachments?.let { read.withAttachmentsKept(it.keptNames) } ?: read
            saveForm(FfiCommands.UPDATE_ENTRY_FROM_FORM_DATA, dbKey, entry, attachments?.added.orEmpty())
        }
    }

    override suspend fun attachmentBytes(dbKey: String, attachmentId: String): ByteArray =
        withContext(dispatcher) {
            val answer = FfiBinaryCall.call(
                command = FfiCommands.ENTRY_ATTACHMENT_BYTES,
                argsJson = encode(AttachmentArgsDto.serializer(), AttachmentArgsDto(dbKey, attachmentId)),
            )
            // As on save: a refusal has no bytes behind it and travels as the failure it is
            FfiEnvelope.unwrap(answer.envelope, AttachmentBytesDto.serializer())

            answer.payload
        }

    /**
     * Inserts or updates [form] with [added] files in one call - across the binary boundary: the form may hold
     * passwords, and the arguments are wiped there; the slot with the files is wiped here, whatever the outcome.
     */
    private fun saveForm(command: String, dbKey: String, form: EntryFormDataDto, added: List<NewAttachmentData>) {
        val slot = attachmentSlot(added)
        try {
            val answer = FfiBinaryCall.call(
                command = command,
                argsJson = encode(EntryFormArgsDto.serializer(), EntryFormArgsDto(dbKey, form, slot.list)),
                input = slot.bytes,
            )
            FfiEnvelope.unwrap(answer.envelope, DoneDto.serializer())
        } finally {
            slot.wipe()
        }
    }

    override suspend fun deleteEntry(dbKey: String, entryId: String) {
        withContext(dispatcher) {
            call(
                FfiCommands.DELETE_ENTRY,
                EntryIdDto.serializer(),
                EntryIdDto(dbKey = dbKey, entryUuid = entryId),
                DoneDto.serializer(),
            )
        }
    }

    override suspend fun moveEntry(dbKey: String, entryId: String, groupId: String) {
        withContext(dispatcher) {
            call(
                FfiCommands.MOVE_ENTRY,
                MoveEntryDto.serializer(),
                MoveEntryDto(dbKey = dbKey, entryUuid = entryId, newParentUuid = groupId),
                DoneDto.serializer(),
            )
        }
    }

    override suspend fun entryHistory(dbKey: String, entryId: String): List<HistoryVersionData> =
        withContext(dispatcher) {
            call(
                FfiCommands.HISTORY_ENTRIES_SUMMARY,
                EntryIdDto.serializer(),
                EntryIdDto(dbKey = dbKey, entryUuid = entryId),
                ListSerializer(EntrySummaryDto.serializer()),
            ).mapIndexed { position, row -> row.toHistoryVersion(position) }
        }

    override suspend fun historyVersion(dbKey: String, entryId: String, index: Int): EntryData =
        withContext(dispatcher) {
            val version = call(
                FfiCommands.HISTORY_ENTRY_BY_INDEX,
                HistoryIndexDto.serializer(),
                HistoryIndexDto(dbKey = dbKey, entryUuid = entryId, index = index),
                EntryFormDataDto.serializer(),
            )
            version.toEntryData()
        }

    override suspend fun restoreHistoryVersion(dbKey: String, entryId: String, index: Int) {
        withContext(dispatcher) {
            // The version comes as a whole form, its group the entry's; sent back as the entry, it is the entry
            // again - and the core puts what was there into the history
            val version = call(
                FfiCommands.HISTORY_ENTRY_BY_INDEX,
                HistoryIndexDto.serializer(),
                HistoryIndexDto(dbKey = dbKey, entryUuid = entryId, index = index),
                EntryFormDataDto.serializer(),
            )
            // Its attachments too: the bytes of a version are in the database while the version links them
            saveForm(FfiCommands.UPDATE_ENTRY_FROM_FORM_DATA, dbKey, version, emptyList())
        }
    }

    override suspend fun deleteHistoryVersion(dbKey: String, entryId: String, index: Int) {
        withContext(dispatcher) {
            call(
                FfiCommands.DELETE_HISTORY_ENTRY_BY_INDEX,
                HistoryIndexDto.serializer(),
                HistoryIndexDto(dbKey = dbKey, entryUuid = entryId, index = index),
                DoneDto.serializer(),
            )
        }
    }

    override suspend fun deleteHistory(dbKey: String, entryId: String) {
        withContext(dispatcher) {
            call(
                FfiCommands.DELETE_HISTORY_ENTRIES,
                EntryIdDto.serializer(),
                EntryIdDto(dbKey = dbKey, entryUuid = entryId),
                DoneDto.serializer(),
            )
        }
    }

    private fun entryForm(dbKey: String, entryId: String): EntryFormDataDto = call(
        FfiCommands.GET_ENTRY_FORM_DATA_BY_ID,
        EntryIdDto.serializer(),
        EntryIdDto(dbKey = dbKey, entryUuid = entryId),
        EntryFormDataDto.serializer(),
    )

    /** A call with json both ways and nothing in the binary slot - most of the commands. */
    private fun <A, R> call(
        command: String,
        argsSerializer: kotlinx.serialization.SerializationStrategy<A>,
        args: A,
        answerSerializer: kotlinx.serialization.DeserializationStrategy<R>,
    ): R = FfiEnvelope.unwrap(PassFfi.invoke(command, encode(argsSerializer, args)), answerSerializer)

    private fun <T> encode(serializer: kotlinx.serialization.SerializationStrategy<T>, value: T): String =
        FfiEnvelope.json.encodeToString(serializer, value)

    private fun NewDatabase.toDto() = NewDatabaseDto(
        databaseFileName = dbKey,
        fileName = fileName,
        databaseName = databaseName,
        databaseDescription = databaseDescription,
        password = password,
        keyFile = keyFile?.toDto(),
        // Argon2id and AES-256 are what a new KeePass database is expected to be. The parameters are the
        // core's defaults for now; what they should be on a phone is decided after measuring on a device
        kdf = KdfDto(
            algorithm = ARGON2ID,
            memory = ARGON2_MEMORY_BYTES,
            iterations = ARGON2_ITERATIONS,
            parallelism = ARGON2_PARALLELISM,
        ),
        cipherId = AES_256,
    )

    private fun KdbxLoadedDto.toDomain() = OpenedDatabase(
        dbKey = dbKey,
        databaseName = databaseName,
        fileName = fileName,
        keyFileName = keyFileName,
    )

    /**
     * Refused here before encoding, not only by the core: base64 of a picked video would be a string of a
     * dozen megabytes built for nothing. The same kind as the core's refusal, so a caller has one to handle.
     */
    private fun KeyFile.toDto(): KeyFileDto {
        if (CredentialLimits.isKeyFileTooLarge(content.size.toLong())) {
            throw CredentialLimits.keyFileTooLarge()
        }
        return KeyFileDto(name = name, content = Base64.getEncoder().encodeToString(content))
    }

    private fun MergeResultDto.toDomain() = MergeSummary(
        addedGroups = addedGroups.size,
        updatedGroups = updatedGroups.size,
        movedGroups = parentChangedGroups.size,
        addedEntries = addedEntries.size,
        updatedEntries = updatedEntries.size,
        movedEntries = parentChangedEntries.size,
        deletedGroups = permanentlyDeletedGroups.size,
        deletedEntries = permanentlyDeletedEntries.size,
        metaDataChanged = metaDataChanged,
        mergeDone = mergeDone,
        differentDatabases = differentDatabases,
    )

    private fun PasswordOptions.toDto() = PasswordOptionsDto(
        length = length,
        numbers = numbers,
        lowercaseLetters = lowercaseLetters,
        uppercaseLetters = uppercaseLetters,
        symbols = symbols,
        spaces = spaces,
        excludeSimilarCharacters = excludeSimilarCharacters,
        strict = strict,
    )

    private companion object {
        // Tags of the core's enums, as its json spells them
        const val ARGON2ID = "Argon2id"
        const val AES_256 = "Aes256"

        // A history limit the core takes as none, both of the count and of the size
        const val NO_LIMIT = -1L

        // Argon2id parameters of a new database. Measured on a device, not guessed: with the core's
        // defaults (64 MiB, 10 iterations) deriving the key took about 8 seconds - on opening and on
        // every save alike, because KeePass draws a new master seed before each write.
        //
        // Iterations are what came down, memory stayed. Memory is what makes guessing expensive on a
        // gpu or on purpose built hardware; iterations only cost the cpu, and they cost it on the one
        // machine that is waiting - the phone in a hand. 64 MiB with 3 passes is one of the two
        // configurations RFC 9106 recommends.
        //
        // These are the parameters of a database this app creates. A database brought from elsewhere
        // keeps its own, which is why changing them needs the settings screen, not this constant -
        // plan/todo/android/kdf-and-perf.md.
        const val ARGON2_MEMORY_BYTES = 67_108_864L
        const val ARGON2_ITERATIONS = 3L
        const val ARGON2_PARALLELISM = 2
    }
}
