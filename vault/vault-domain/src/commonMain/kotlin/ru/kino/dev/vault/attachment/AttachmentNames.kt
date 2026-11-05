package ru.kino.dev.vault.attachment

/**
 * Names of attachments: one name is one attachment of the entry, the core refuses a second (pass-docs, Step 102). A
 * file added under a name the entry has already gets a number, as the system gives a copy (pass-docs, Step 103):
 * `photo.jpg` - `photo (2).jpg`, then `(3)`; quietly, nothing is asked.
 */
object AttachmentNames {

    /** The name given to a file whose provider tells none. */
    const val UNNAMED: String = "attachment"

    /** [name], or the first of it with a number that is not in [taken]. A blank name is [UNNAMED]. */
    fun unique(name: String, taken: Collection<String>): String {
        val wanted = name.ifBlank { UNNAMED }
        if (wanted !in taken) return wanted
        // The number goes before the extension; a name starting with its only dot (`.env`) has none
        val dot = wanted.lastIndexOf('.').takeIf { it > 0 }
        val base = dot?.let { wanted.substring(0, it) } ?: wanted
        val extension = dot?.let { wanted.substring(it) }.orEmpty()
        return generateSequence(FIRST_COPY) { it + 1 }
            .map { "$base ($it)$extension" }
            .first { it !in taken }
    }

    private const val FIRST_COPY = 2
}
