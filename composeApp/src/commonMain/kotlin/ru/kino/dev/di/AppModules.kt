package ru.kino.dev.di

import ru.kino.dev.settings.data.settingsDataModule
import ru.kino.dev.settings.pres.di.settingsPresModule
import ru.kino.dev.vault.data.vaultDataModule
import ru.kino.dev.vault.pres.di.vaultPresModule
import ru.kino.dev.welcome.data.welcomeDataModule
import ru.kino.dev.welcome.pres.di.welcomePresModule

/**
 * The graph of the app: every module brings its own (pass-docs, Step 84). Koin checks it when a class is asked
 * for, not when the app compiles - `AppGraphTest` does what the compiler of Hilt did.
 *
 * All but `coreDataModule`: it is of the jvm of both platforms (`jvmShared` of core-data), out of sight of common
 * code - the app of each platform adds it (pass-docs, Step 88).
 */
val appModules = listOf(
    corePresModule,
    coreNavModule,
    welcomeDataModule,
    welcomePresModule,
    vaultDataModule,
    vaultPresModule,
    settingsDataModule,
    settingsPresModule,
)
