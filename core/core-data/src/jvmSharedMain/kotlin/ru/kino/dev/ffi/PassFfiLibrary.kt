package ru.kino.dev.ffi

/**
 * Loads the native library of the core (`pass_ffi`) into the process.
 *
 * Throws [UnsatisfiedLinkError] if the library is missing - Gradle builds it as part of the app (the
 * `rust-bridge-android` and `rust-bridge-desktop` convention plugins), so that means a broken build.
 */
internal expect fun loadPassFfiLibrary()
