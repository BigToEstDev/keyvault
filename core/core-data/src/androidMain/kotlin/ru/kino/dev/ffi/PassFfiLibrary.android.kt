package ru.kino.dev.ffi

// lib/<abi>/libkeyvault.so of the apk, put there by the rust-bridge-android convention plugin
internal actual fun loadPassFfiLibrary() {
    System.loadLibrary(NATIVE_LIBRARY)
}
