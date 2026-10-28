package ru.kino.dev.desktop

import java.io.File

// Set by compose desktop for `run` and for the installed app alike: the per-platform app resources,
// where `rust-bridge-desktop` puts the library
private const val RESOURCES_DIR_PROPERTY = "compose.application.resources.dir"

/**
 * Loads the Rust bridge (`pass_ffi`) from the app resources.
 *
 * Only the load for now: the JNI class of the bridge lives in the android `core-data` and moves to the
 * shared `jvmShared` source set in Step 86 cmp (pass-docs), which brings the first real call and replaces
 * this loader with the `expect` / `actual` one.
 */
object PassFfiLibrary {

    fun load(): Result<File> = runCatching {
        val resourcesDir = System.getProperty(RESOURCES_DIR_PROPERTY)
            ?: error("$RESOURCES_DIR_PROPERTY is not set: start the app through compose desktop (run / installer)")
        val library = File(resourcesDir, System.mapLibraryName("pass_ffi"))
        System.load(library.absolutePath)
        library
    }
}
