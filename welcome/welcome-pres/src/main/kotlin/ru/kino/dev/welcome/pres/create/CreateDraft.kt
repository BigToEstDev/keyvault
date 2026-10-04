package ru.kino.dev.welcome.pres.create

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * What the screens of creating a database hand on to each other: the password is set on the first one and
 * the database is created on the last (pass-docs, Step 27, `CreateDraft`).
 *
 * Scoped to the creation graph: it lives exactly as long as the graph is on the back stack, and is cleared
 * when the graph goes - "back" from the master password, or the database created. In memory only: not a
 * navigation argument and not saved state, which may end up on disk. After the process was killed it is
 * empty, and creating starts over.
 */
@HiltViewModel
class CreateDraft @Inject constructor() : ViewModel() {

    /** Null until set, and again after the process was killed. */
    var password: String? = null
        private set

    var withKeyFile: Boolean = false
        private set

    /** The key file already written, which the database is created with. */
    var keyFileUri: String? = null
        private set

    fun start(password: String, withKeyFile: Boolean) {
        this.password = password
        this.withKeyFile = withKeyFile
        keyFileUri = null
    }

    fun keyFileSaved(uri: String) {
        keyFileUri = uri
    }

    // A String cannot be wiped - dropping the reference is what there is
    override fun onCleared() {
        password = null
        keyFileUri = null
    }
}

/** Names "save as" suggests; the user may change them. */
internal object CreateFileNames {
    const val DATABASE = "kdbxvault.kdbx"
    const val KEY_FILE = "kdbxvault.keyx"
}
