package ru.kino.dev.welcome.pres.di

import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import ru.kino.dev.navigation.Navigator
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
 * lambda, the rest is the graph's. The navigator is the screen's too - `LocalNavigator`, not the graph's: inside a
 * container it is the container's (pass-docs, Step 98).
 */
val welcomePresModule = module {
    factoryOf(::BiometricSignIn)
    factory { (navigator: Navigator) -> CreateSteps(get(), get(), get(), navigator, get()) }

    viewModelOf(::WelcomeStartResolver)
    viewModel { (navigator: Navigator) -> WelcomeProcessor(navigator) }
    // One for the creation graph: the screens take it from the entry of the graph (WelcomeNavigation)
    viewModelOf(::CreateDraft)

    viewModel { (pickedUri: String?, navigator: Navigator) ->
        UnlockProcessor(pickedUri, get(), get(), get(), navigator = navigator, get(), get(), get())
    }
    viewModel { (dbKey: String, navigator: Navigator) ->
        LockedProcessor(dbKey, get(), get(), get(), navigator = navigator, get(), get(), get())
    }
    viewModel { (draft: CreateDraft, navigator: Navigator) ->
        MasterPasswordProcessor(draft, get { parametersOf(navigator) }, navigator)
    }
    viewModel { (draft: CreateDraft, navigator: Navigator) ->
        KeyFileBackupProcessor(draft, get { parametersOf(navigator) }, navigator)
    }
    viewModel { (dbKey: String, draft: CreateDraft, navigator: Navigator) ->
        BiometricSetupProcessor(dbKey, draft, get(), navigator, get())
    }
}
