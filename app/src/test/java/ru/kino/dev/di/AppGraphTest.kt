package ru.kino.dev.di

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.definition
import org.koin.test.verify.injectedParameters
import org.koin.test.verify.verify
import ru.kino.dev.settings.pres.appearance.AppearanceProcessor
import ru.kino.dev.settings.pres.entries.EntriesProcessor
import ru.kino.dev.settings.pres.file.FileProcessor
import ru.kino.dev.settings.pres.lock.LockProcessor
import ru.kino.dev.settings.pres.presetedit.PresetEditProcessor
import ru.kino.dev.settings.pres.recyclebin.RecycleBinProcessor
import ru.kino.dev.settings.pres.security.SecurityProcessor
import ru.kino.dev.settings.pres.settings.SettingsProcessor
import ru.kino.dev.vault.pres.entryedit.EntryEditProcessor
import ru.kino.dev.vault.pres.entryhistory.EntryHistoryProcessor
import ru.kino.dev.vault.pres.entryread.EntryReadProcessor
import ru.kino.dev.vault.pres.entryversion.EntryVersionProcessor
import ru.kino.dev.vault.pres.grouplist.GroupListProcessor
import ru.kino.dev.vault.pres.trash.TrashProcessor
import ru.kino.dev.vault.pres.trashentry.TrashEntryProcessor
import ru.kino.dev.welcome.pres.biometricsetup.BiometricSetupProcessor
import ru.kino.dev.welcome.pres.create.CreateDraft
import ru.kino.dev.welcome.pres.keyfilebackup.KeyFileBackupProcessor
import ru.kino.dev.welcome.pres.locked.LockedProcessor
import ru.kino.dev.welcome.pres.masterpassword.MasterPasswordProcessor
import ru.kino.dev.welcome.pres.unlock.UnlockProcessor

/**
 * Koin builds the graph when a class is asked for, not when the app compiles as Hilt did - a binding missing is a
 * crash on the screen that needs it. This is what the compiler of Hilt checked: every definition of the app finds
 * what its constructor asks for, and every processor is given by its screen what the graph does not have
 * (pass-docs, Step 84).
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
                // What each screen gives its processor - parametersOf on the screen
                definition<UnlockProcessor>(String::class),
                definition<LockedProcessor>(String::class),
                definition<MasterPasswordProcessor>(CreateDraft::class),
                definition<KeyFileBackupProcessor>(CreateDraft::class),
                definition<BiometricSetupProcessor>(String::class, CreateDraft::class),
                definition<GroupListProcessor>(String::class),
                definition<EntryReadProcessor>(String::class),
                definition<EntryEditProcessor>(String::class),
                definition<EntryHistoryProcessor>(String::class),
                definition<EntryVersionProcessor>(String::class, Int::class),
                definition<TrashProcessor>(String::class),
                definition<TrashEntryProcessor>(String::class),
                definition<SettingsProcessor>(String::class),
                definition<AppearanceProcessor>(String::class),
                definition<EntriesProcessor>(String::class),
                definition<FileProcessor>(String::class),
                definition<LockProcessor>(String::class),
                definition<RecycleBinProcessor>(String::class),
                definition<SecurityProcessor>(String::class),
                definition<PresetEditProcessor>(String::class),
            ),
        )
    }
}
