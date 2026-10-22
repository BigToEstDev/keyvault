package ru.kino.dev.ffi

/**
 * The names of the commands, as the bridge knows them.
 *
 * The same list lives in `rust/pass-ffi/src/commands.rs`, and Rust writes it out to
 * `rust/pass-ffi/contract/commands.json`. `FfiCommandsTest` reads that file and compares it with [ALL],
 * because a misspelled name here compiles perfectly and fails only on a device.
 *
 * Adding a command means: a variant in Rust, a branch in its dispatcher, the regenerated contract file,
 * and an entry here.
 */
internal object FfiCommands {

    // The life cycle of a database file. The names are the core's own method names
    const val CREATE_DATABASE = "create_and_write_to_writer"
    const val READ_DATABASE = "read_kdbx"
    const val SAVE_DATABASE = "save_kdbx_to_writer"
    const val CLOSE_DATABASE = "close_kdbx"
    const val LOCK_DATABASE = "lock_kdbx"
    const val UNLOCK_DATABASE = "unlock_kdbx"
    const val IS_DATABASE_LOCKED = "is_db_locked"
    const val IS_DATABASE_OPENED = "is_db_opened"
    const val RENAME_DB_KEY = "rename_db_key"
    const val CONTEXT_STATUSES = "kdbx_context_statuses"

    // Follows a biometric prompt that already succeeded on this side
    const val UNLOCK_ON_BIOMETRIC = "unlock_kdbx_on_biometric_authentication"

    // The password of an open database checked alone, and what its key is made of (pass-docs, Step 60)
    const val VERIFY_PASSWORD = "verify_password"
    const val CREDENTIALS_USED = "credentials_used"

    // Groups: the tree the ui draws, one group for its edit screen, and the operations that change it
    const val GROUPS_SUMMARY_DATA = "groups_summary_data"
    const val GET_GROUP_BY_ID = "get_group_by_id"
    const val NEW_BLANK_GROUP = "new_blank_group"
    const val NEW_BLANK_GROUP_WITH_PARENT = "new_blank_group_with_parent"
    const val INSERT_GROUP = "insert_group"
    const val UPDATE_GROUP = "update_group"
    const val MOVE_GROUP = "move_group"
    const val SORT_SUB_GROUPS = "sort_sub_groups"
    const val CLONE_GROUP = "clone_group"
    // Into the recycle bin go the entries of its subtree, the groups go for good; all for good without a bin
    const val DELETE_GROUP = "delete_group"

    // Entries: the list of a category, the form behind one entry, and what changes it
    const val ENTRY_SUMMARY_DATA = "entry_summary_data"
    const val GET_ENTRY_FORM_DATA_BY_ID = "get_entry_form_data_by_id"
    const val ENTRY_KEY_VALUE_FIELDS = "entry_key_value_fields"
    const val NEW_ENTRY_FORM_DATA = "new_entry_form_data"
    const val INSERT_ENTRY_FROM_FORM_DATA = "insert_entry_from_form_data"
    const val UPDATE_ENTRY_FROM_FORM_DATA = "update_entry_from_form_data"
    const val MOVE_ENTRY = "move_entry"
    const val CLONE_ENTRY = "clone_entry"
    // Into the recycle bin, or for good when the database does not use one
    const val DELETE_ENTRY = "delete_entry"

    // The history of an entry: kdbx keeps old versions inside the entry, addressed by index
    const val HISTORY_ENTRIES_SUMMARY = "history_entries_summary"
    const val HISTORY_ENTRY_BY_INDEX = "history_entry_by_index"
    const val DELETE_HISTORY_ENTRY_BY_INDEX = "delete_history_entry_by_index"
    const val DELETE_HISTORY_ENTRIES = "delete_history_entries"

    // Two factor codes: the tokens a list shows, and the 2fa settings of one entry
    const val ENTRY_LIST_CURRENT_OTPS = "entry_list_current_otps"
    const val FORM_OTP_URL = "form_otp_url"
    const val IS_VALID_OTP_URL = "is_valid_otp_url"
    const val SET_ENTRY_OTP = "set_entry_otp"
    const val DELETE_ENTRY_OTP = "delete_entry_otp"

    // Search over every field of every entry, and the tags a picker offers
    const val SEARCH_TERM = "search_term"
    const val COLLECT_ENTRY_GROUP_TAGS = "collect_entry_group_tags"

    // The settings of a database, and the key file the core writes by path
    const val GET_DB_SETTINGS = "get_db_settings"
    const val SET_DB_SETTINGS = "set_db_settings"
    // One KDF pass of an open database timed on this device - the open time a settings screen shows (pass-docs, Step 68)
    const val MEASURE_KDF_PASS = "measure_kdf_pass"
    const val GENERATE_KEY_FILE = "generate_key_file"

    // Has the file changed under us, and merging it back in when it has. All but one carry the bytes
    // of the file on disk, so they go through invokeBinary
    const val VERIFY_DB_FILE_CHECKSUM = "verify_db_file_checksum"
    const val SET_DB_FILE_CHECKSUM = "calculate_and_set_db_file_checksum"
    const val DB_CHECKSUM_HASH = "db_checksum_hash"
    const val MERGE_DATABASE = "merge_kdbx_with_reader"

    // The recycle bin holds entries only (pass-docs, Step 69): its list and search, restoring to a group, and
    // the irreversible steps - removing picked entries and emptying it
    const val TRASH_ENTRIES = "trash_entries"
    const val SEARCH_TRASH = "search_trash"
    const val RESTORE_ENTRIES = "restore_entries"
    const val REMOVE_ENTRIES_PERMANENTLY = "remove_entries_permanently"
    const val EMPTY_TRASH = "empty_trash"

    const val GENERATE_PASSWORD = "generate_password"
    const val ANALYZED_PASSWORD = "analyzed_password"

    /** Every name this side knows. Kept in sync with the contract file by the test. */
    val ALL: Set<String> = setOf(
        CREATE_DATABASE,
        READ_DATABASE,
        SAVE_DATABASE,
        CLOSE_DATABASE,
        LOCK_DATABASE,
        UNLOCK_DATABASE,
        IS_DATABASE_LOCKED,
        IS_DATABASE_OPENED,
        RENAME_DB_KEY,
        CONTEXT_STATUSES,
        UNLOCK_ON_BIOMETRIC,
        VERIFY_PASSWORD,
        CREDENTIALS_USED,
        GROUPS_SUMMARY_DATA,
        GET_GROUP_BY_ID,
        NEW_BLANK_GROUP,
        NEW_BLANK_GROUP_WITH_PARENT,
        INSERT_GROUP,
        UPDATE_GROUP,
        MOVE_GROUP,
        SORT_SUB_GROUPS,
        CLONE_GROUP,
        DELETE_GROUP,
        ENTRY_SUMMARY_DATA,
        GET_ENTRY_FORM_DATA_BY_ID,
        ENTRY_KEY_VALUE_FIELDS,
        NEW_ENTRY_FORM_DATA,
        INSERT_ENTRY_FROM_FORM_DATA,
        UPDATE_ENTRY_FROM_FORM_DATA,
        MOVE_ENTRY,
        CLONE_ENTRY,
        DELETE_ENTRY,
        HISTORY_ENTRIES_SUMMARY,
        HISTORY_ENTRY_BY_INDEX,
        DELETE_HISTORY_ENTRY_BY_INDEX,
        DELETE_HISTORY_ENTRIES,
        ENTRY_LIST_CURRENT_OTPS,
        FORM_OTP_URL,
        IS_VALID_OTP_URL,
        SET_ENTRY_OTP,
        DELETE_ENTRY_OTP,
        SEARCH_TERM,
        COLLECT_ENTRY_GROUP_TAGS,
        GET_DB_SETTINGS,
        SET_DB_SETTINGS,
        MEASURE_KDF_PASS,
        GENERATE_KEY_FILE,
        VERIFY_DB_FILE_CHECKSUM,
        SET_DB_FILE_CHECKSUM,
        DB_CHECKSUM_HASH,
        MERGE_DATABASE,
        TRASH_ENTRIES,
        SEARCH_TRASH,
        RESTORE_ENTRIES,
        REMOVE_ENTRIES_PERMANENTLY,
        EMPTY_TRASH,
        GENERATE_PASSWORD,
        ANALYZED_PASSWORD,
    )
}
