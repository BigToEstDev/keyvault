package ru.kino.dev.storage

import ru.kino.dev.core.DatabaseFileException

/**
 * Whether a write that replaced a file left exactly the bytes written in it.
 *
 * "wt" is a request to the provider, not a promise: one that ignores the "t" keeps the tail of a longer file
 * after the new bytes, and the database is broken without a word (pass-docs, Step 112;
 * docs/app/decisions.md, "Запись файла базы").
 */
internal object ReplacedContent {

    /**
     * @param sizeAfterWrite the size of the file once it was cut to the bytes written, or null when the
     * descriptor was not a file (a pipe): then neither the cut nor the size are there, and the file is read back.
     */
    suspend fun verify(uri: String, bytes: ByteArray, sizeAfterWrite: Long?, readBack: suspend () -> ByteArray) {
        val written = if (sizeAfterWrite != null) {
            sizeAfterWrite == bytes.size.toLong()
        } else {
            readBack().contentEquals(bytes)
        }
        if (!written) throw DatabaseFileException("The file does not hold what was written to it: $uri")
    }
}
