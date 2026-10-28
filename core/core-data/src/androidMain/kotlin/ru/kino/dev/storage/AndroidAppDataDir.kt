package ru.kino.dev.storage

import android.content.Context

/** `filesDir` of the app - where `preferencesDataStore` kept its files before (pass-docs, Step 86). */
internal class AndroidAppDataDir(context: Context) : AppDataDir {

    override val path: String = context.filesDir.absolutePath
}
