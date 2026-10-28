package ru.kino.dev.di

import org.koin.core.module.Module
import org.koin.dsl.module
import ru.kino.dev.ffi.ffiModule
import ru.kino.dev.phone.phoneModule
import ru.kino.dev.preset.presetModule

/** The core, the files of databases, the presets and the phone numbers - for every feature. */
val coreDataModule = module {
    includes(ffiModule, platformCoreDataModule, presetModule, phoneModule)
}

/** [ru.kino.dev.core.DatabaseFiles] and [ru.kino.dev.storage.AppDataDir] of the platform. */
internal expect val platformCoreDataModule: Module
