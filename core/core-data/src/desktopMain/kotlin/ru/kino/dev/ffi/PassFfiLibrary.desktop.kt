package ru.kino.dev.ffi

import java.io.File

/**
 * Compose desktop puts the app resources of the current platform into the directory named by this property -
 * both for `run` and in an installed app; rust-bridge-desktop puts the library there.
 */
private const val RESOURCES_DIR_PROPERTY = "compose.application.resources.dir"

// The library from the app resources, by its full path: java.library.path is not ours to rely on
internal actual fun loadPassFfiLibrary() {
    val resourcesDir = System.getProperty(RESOURCES_DIR_PROPERTY)
        ?: throw UnsatisfiedLinkError("$RESOURCES_DIR_PROPERTY is not set: the app was not started by compose desktop")
    System.load(File(resourcesDir, System.mapLibraryName(NATIVE_LIBRARY)).absolutePath)
}
