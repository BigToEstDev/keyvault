package ru.kino.dev.ffi

/**
 * Loads the native library of the core ([NATIVE_LIBRARY]) into the process.
 *
 * Throws [UnsatisfiedLinkError] if the library is missing - Gradle builds it as part of the app (the
 * `rust-bridge-android` and `rust-bridge-desktop` convention plugins), so that means a broken build.
 */
internal expect fun loadPassFfiLibrary()

/**
 * Name of the native library - `[lib] name` of `rust/pass-ffi/Cargo.toml`; the platform adds the rest
 * (`libkeyvault.so`, `keyvault.dll`). The desktop build plugin (`rust-bridge-desktop`) names it again.
 */
internal const val NATIVE_LIBRARY = "keyvault"
