package ru.kino.dev.di

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.definition
import org.koin.test.verify.injectedParameters
import org.koin.test.verify.verify
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.navigation.PresetEditRoute
import ru.kino.dev.settings.pres.appearance.AppearanceProcessor
import ru.kino.dev.settings.pres.entries.EntriesProcessor
import ru.kino.dev.settings.pres.file.FileProcessor
import ru.kino.dev.settings.pres.lock.LockProcessor
import ru.kino.dev.settings.pres.presetedit.PresetEditProcessor
import ru.kino.dev.settings.pres.presetread.PresetReadProcessor
import ru.kino.dev.settings.pres.presets.PresetsProcessor
import ru.kino.dev.settings.pres.presetscontainer.PresetsContainerProcessor
import ru.kino.dev.settings.pres.privacy.PrivacyProcessor
import ru.kino.dev.settings.pres.recyclebin.RecycleBinProcessor
import ru.kino.dev.settings.pres.security.SecurityProcessor
import ru.kino.dev.settings.pres.settings.SettingsProcessor
import ru.kino.dev.vault.pres.container.VaultContainerProcessor
import ru.kino.dev.vault.pres.entryedit.EntryEditProcessor
import ru.kino.dev.vault.pres.entryhistory.EntryHistoryProcessor
import ru.kino.dev.vault.pres.entryread.EntryReadProcessor
import ru.kino.dev.vault.pres.entryversion.EntryVersionProcessor
import ru.kino.dev.vault.pres.grouplist.GroupListProcessor
import ru.kino.dev.vault.pres.trash.TrashProcessor
import ru.kino.dev.vault.pres.trashcontainer.TrashContainerProcessor
import ru.kino.dev.vault.pres.trashentry.TrashEntryProcessor
import ru.kino.dev.welcome.pres.biometricsetup.BiometricSetupProcessor
import ru.kino.dev.welcome.pres.create.CreateDraft
import ru.kino.dev.welcome.pres.create.CreateSteps
import ru.kino.dev.welcome.pres.keyfilebackup.KeyFileBackupProcessor
import ru.kino.dev.welcome.pres.locked.LockedProcessor
import ru.kino.dev.welcome.pres.masterpassword.MasterPasswordProcessor
import ru.kino.dev.welcome.pres.unlock.UnlockProcessor
import ru.kino.dev.welcome.pres.welcome.WelcomeProcessor

/**
 * Koin builds the graph when a class is asked for, not when the app compiles as Hilt did - a binding missing is a
 * crash on the screen that needs it. This is what the compiler of Hilt checked: every definition of the app finds
 * what its constructor asks for, and every processor is given by its screen what the graph does not have
 * (pass-docs, Step 84). The navigator too: the graph has none - a processor takes the one of its screen (Step 98).
 *
 * Static: the constructors are read, nothing is made - the native core and the stores stay untouched.
 */
@OptIn(KoinExperimentalAPI::class)
class AppGraphTest {

    @Test
    fun `every definition finds what its constructor asks for`() {
        // One module with all of them: verifyAll would check each alone, without what the others give
        module { includes(appModules + coreDataModule) }.verify(
            extraTypes = listOf(
                // androidContext() of KeyVaultApp
                Context::class,
                // Made inside the definitions of the lock and the timer of inactivity, not taken from the graph
                CoroutineScope::class,
                Function0::class,
                Function1::class,
            ),
            injections = injectedParameters(
                // What each screen gives its processor - parametersOf on the screen, the navigator of the screen last
                definition<WelcomeProcessor>(Navigator::class),
                definition<UnlockProcessor>(String::class, Navigator::class),
                definition<LockedProcessor>(String::class, Navigator::class),
                definition<CreateSteps>(Navigator::class),
                definition<MasterPasswordProcessor>(CreateDraft::class, Navigator::class),
                definition<KeyFileBackupProcessor>(CreateDraft::class, Navigator::class),
                definition<BiometricSetupProcessor>(String::class, CreateDraft::class, Navigator::class),
                definition<VaultContainerProcessor>(String::class, Navigator::class),
                definition<GroupListProcessor>(String::class, Navigator::class),
                definition<EntryReadProcessor>(String::class, Navigator::class),
                definition<EntryEditProcessor>(String::class, Navigator::class),
                definition<EntryHistoryProcessor>(String::class, Navigator::class),
                definition<EntryVersionProcessor>(String::class, Int::class, Navigator::class),
                definition<TrashProcessor>(String::class, Navigator::class),
                definition<TrashEntryProcessor>(String::class, Navigator::class),
                definition<TrashContainerProcessor>(String::class, Navigator::class),
                definition<SettingsProcessor>(String::class, Navigator::class),
                definition<AppearanceProcessor>(String::class, Navigator::class),
                definition<EntriesProcessor>(String::class, Navigator::class),
                definition<FileProcessor>(String::class, Navigator::class),
                definition<LockProcessor>(String::class, Navigator::class),
                definition<RecycleBinProcessor>(String::class, Navigator::class),
                definition<SecurityProcessor>(String::class, Navigator::class),
                definition<PresetsProcessor>(Navigator::class),
                definition<PrivacyProcessor>(Navigator::class),
                definition<PresetEditProcessor>(String::class, Navigator::class),
                definition<PresetReadProcessor>(String::class, Navigator::class),
                definition<PresetsContainerProcessor>(PresetEditRoute::class, Navigator::class),
            ),
        )
    }
}
