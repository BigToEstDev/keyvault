package ru.kino.dev.storage

import java.io.File

/**
 * `KeyVault` in the directory each system keeps the data of an app of the user in: AppData, Application Support,
 * XDG. Only the settings live here, never a database (pass-docs, Step 86).
 */
internal class DesktopAppDataDir : AppDataDir {

    override val path: String by lazy {
        val home = System.getProperty("user.home")
        val os = System.getProperty("os.name").lowercase()
        val base = when {
            os.contains("win") -> System.getenv("APPDATA") ?: File(home, "AppData/Roaming").path
            os.contains("mac") -> "$home/Library/Application Support"
            else -> System.getenv("XDG_DATA_HOME") ?: "$home/.local/share"
        }
        File(base, APP_DIR).apply { mkdirs() }.absolutePath
    }

    private companion object {
        const val APP_DIR = "KeyVault"
    }
}
