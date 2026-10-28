package ru.kino.dev.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

/**
 * A preferences store of the app named [name], in the file `preferencesDataStore(name)` of android used: the
 * settings kept before the move to Compose Multiplatform are read on (pass-docs, Step 86).
 *
 * One store per file for the whole process - two on one file make DataStore fail - so every store is a single of
 * the graph, under the qualifier of its name; never call this twice for one name.
 */
fun AppDataDir.preferencesStore(name: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { "$path/datastore/$name.preferences_pb".toPath() })
