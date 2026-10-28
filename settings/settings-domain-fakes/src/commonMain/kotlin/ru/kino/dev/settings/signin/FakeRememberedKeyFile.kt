package ru.kino.dev.settings.signin

/**
 * The remembered key file in memory: every [remember] is written to [journal] as `"remember <dbUri> <keyFileUri>"`
 * (`null` for none). [failure], when set, is thrown instead, after the call is journaled.
 */
class FakeRememberedKeyFile(private val journal: MutableList<String> = mutableListOf()) : RememberedKeyFile {

    var failure: Exception? = null

    /** The key file name last remembered. */
    var keyFileName: String? = null

    override suspend fun remember(dbUri: String, keyFileUri: String?, keyFileName: String?) {
        journal += "remember $dbUri $keyFileUri"
        failure?.let { throw it }
        this.keyFileName = keyFileName
    }
}
