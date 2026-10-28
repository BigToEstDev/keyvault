package ru.kino.dev.welcome.pres.di

import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ru.kino.dev.welcome.pres.biometricsetup.BiometricSetupProcessor
import ru.kino.dev.welcome.pres.common.BiometricSignIn
import ru.kino.dev.welcome.pres.create.CreateDraft
import ru.kino.dev.welcome.pres.create.CreateSteps
import ru.kino.dev.welcome.pres.keyfilebackup.KeyFileBackupProcessor
import ru.kino.dev.welcome.pres.locked.LockedProcessor
import ru.kino.dev.welcome.pres.masterpassword.MasterPasswordProcessor
import ru.kino.dev.welcome.pres.start.WelcomeStartResolver
import ru.kino.dev.welcome.pres.unlock.UnlockProcessor
import ru.kino.dev.welcome.pres.welcome.WelcomeProcessor

/**
 * The screens of `welcome`. What a screen gives its processor (`parametersOf` on the screen) comes first in the
 * lambda, the rest is the graph's.
 */
val welcomePresModule = module {
    factoryOf(::BiometricSignIn)
    factoryOf(::CreateSteps)

    viewModelOf(::WelcomeStartResolver)
    viewModelOf(::WelcomeProcessor)
    // One for the creation graph: the screens take it from the entry of the graph (WelcomeNavigation)
    viewModelOf(::CreateDraft)

    viewModel { (pickedUri: String?) -> UnlockProcessor(pickedUri, get(), get(), get(), get(), get(), get(), get()) }
    viewModel { (dbKey: String) -> LockedProcessor(dbKey, get(), get(), get(), get(), get(), get(), get()) }
    viewModel { (draft: CreateDraft) -> MasterPasswordProcessor(draft, get(), get()) }
    viewModel { (draft: CreateDraft) -> KeyFileBackupProcessor(draft, get(), get()) }
    viewModel { (dbKey: String, draft: CreateDraft) -> BiometricSetupProcessor(dbKey, draft, get(), get(), get()) }
}
