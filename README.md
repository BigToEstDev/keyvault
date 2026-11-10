# KDBX Vault

An offline password manager for KeePass (KDBX 4) databases, for Android and desktop (Windows, macOS, Linux).
One Compose Multiplatform code base; the database engine is
[kdbx-rust-core](https://github.com/BigToEstDev/kdbx-rust-core), linked through the Rust bridge in
`rust/pass-ffi`.

The database stays a file you choose: no account, no network, no cloud. The file opens in other KeePass apps
(KeePassXC, KeePassDX, KeePass) as well.

## Building

The core is a path dependency: check out `kdbx-rust-core` next to this repository, in a folder named
`pass-rust-core`.

```
<parent>/
  pass-app/        this repository
  pass-rust-core/  kdbx-rust-core
```

## Licence

GPL-3.0 — see `LICENSE`. The core descends from
[onekeepass-core](https://github.com/OneKeePass/onekeepass-core) by jeyasankar (GPLv3) and is linked into the
app, so the app and every build of it are under the GPLv3 too.

The licence covers the code, not the name: "KDBX Vault" and its logo are not licensed under the GPLv3. A fork
is welcome under its own name.
