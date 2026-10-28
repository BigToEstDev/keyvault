package ru.kino.dev.ffi

// lib/<abi>/libpass_ffi.so of the apk, put there by the rust-bridge-android convention plugin
internal actual fun loadPassFfiLibrary() {
    System.loadLibrary("pass_ffi")
}
