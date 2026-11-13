# KeyVault

An offline password manager for KeePass (KDBX 4) databases, for Android and desktop.

**Status:** in development. Releases are published on the
[Releases](https://github.com/BigToEstDev/keyvault/releases) page of this repository.

## Features

- Create a database, or open an existing one with a password, a key file, or both.
- Entries and groups: custom fields, attachments, history, recycle bin.
- Search, password generator and password strength check.
- Unlock with biometrics on Android; the database locks after a period of inactivity.
- Local sync without a network: merge a copy of the database in either direction, import another database,
  save or share a copy.
- One code base for Android and desktop (Compose Multiplatform); the database engine is written in Rust.

## Platforms

| Platform | Requirements | Status |
|---|---|---|
| Android | Android 10 (API 29) or newer; arm64-v8a, armeabi-v7a | supported, tested |
| Windows | x86_64 | supported, tested |
| macOS | — | supported, not tested yet |
| Linux | — | supported, not tested yet |

## Compatibility

The database is a regular KDBX 4 file: it is meant to be opened in KeePassXC, KeePass and KeePassDX as well.
Formats, ciphers and key files are those of the engine,
[kdbx-rust-core](https://github.com/BigToEstDev/kdbx-rust-core#compatibility).

## Repository layout

| Path | Contents |
|---|---|
| `composeApp` | The shared app: navigation and the screens put together for Android and desktop |
| `app` | The Android application |
| `desktopApp` | The desktop application |
| `welcome`, `vault`, `settings` | Feature modules: a closed database, an open database, settings — each split into `domain`, `data` and `pres` |
| `core` | Shared modules: the boundary with the engine, file access, theme, navigation |
| `rust/pass-ffi` | The native bridge to the engine: JNI on Android, a shared library on desktop |
| `build-logic` | Gradle convention plugins |

## Building

Built and tested on Windows x86_64.

| Tool | Version | Notes |
|---|---|---|
| JDK | 17 | downloaded by Gradle (toolchains) when missing |
| Gradle | 9.4.1 | through the wrapper, `gradlew` |
| Android SDK | compile and target 37, build tools 36.0.0 | |
| Android NDK | 28.2.13676358 | pinned in the build |
| Rust | 1.98.1 | pinned in `rust-toolchain.toml` — install it with `rustup toolchain install` |
| cargo-ndk | 4.1.2 | `cargo install cargo-ndk` |

The engine is a path dependency: check out
[kdbx-rust-core](https://github.com/BigToEstDev/kdbx-rust-core) next to this repository, in a folder named
`pass-rust-core`.

```
<parent>/
  pass-app/        this repository
  pass-rust-core/  kdbx-rust-core
```

The version of the engine this repository is built with in CI and in releases is the tag in `core.ref`; check out the
same tag to build exactly what a release was built from.

Gradle builds the bridge itself, for every Android ABI or for the desktop host:

```bash
./gradlew assembleDebug        # Android debug APK
./gradlew :desktopApp:run      # desktop app
./gradlew allTests             # tests of the shared modules
```

## Security and privacy

- Works offline: the Android app has no network permission; nothing is sent anywhere.
- The database stays a file the user chooses: no account, no cloud, no telemetry.

### Verifying the Android APK

Every release APK is signed with the same key. Its certificate SHA-256 fingerprint:

```
27:BD:70:DD:2D:0A:3F:EC:0B:DB:75:C8:C9:5A:B7:CF:88:77:2D:7A:50:85:D0:33:3C:22:41:E0:DF:76:3A:89
```

Check a downloaded APK with `apksigner verify --print-certs <file>.apk` (Android SDK build tools) or an app such as
AppVerifier; a different fingerprint means the file is not ours — do not install it.

**Reporting a vulnerability:** please use
[private vulnerability reporting](https://github.com/BigToEstDev/keyvault/security/advisories/new) on
GitHub, not a public issue. Other bugs and questions go to
[issues](https://github.com/BigToEstDev/keyvault/issues).

## Contributing

Pull requests are not accepted. Bug reports and questions are welcome in issues.

## Licence

GPL-3.0-only — see `LICENSE`.

The app links the GPLv3 engine [kdbx-rust-core](https://github.com/BigToEstDev/kdbx-rust-core), so the app
and every build of it are distributed under the GPLv3.

The licence covers the code, not the name: "KeyVault" and its logo are not licensed under the GPLv3. Forks
are welcome under their own name.
