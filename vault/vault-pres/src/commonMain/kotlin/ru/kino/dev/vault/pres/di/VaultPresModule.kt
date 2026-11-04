package ru.kino.dev.vault.pres.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import ru.kino.dev.navigation.Navigator
import ru.kino.dev.vault.pres.container.VaultContainerProcessor
import ru.kino.dev.vault.pres.entryedit.EntryEditArgs
import ru.kino.dev.vault.pres.entryedit.EntryEditProcessor
import ru.kino.dev.vault.pres.entryhistory.EntryHistoryArgs
import ru.kino.dev.vault.pres.entryhistory.EntryHistoryProcessor
import ru.kino.dev.vault.pres.entryread.EntryReadArgs
import ru.kino.dev.vault.pres.entryread.EntryReadProcessor
import ru.kino.dev.vault.pres.entryversion.EntryVersionArgs
import ru.kino.dev.vault.pres.entryversion.EntryVersionProcessor
import ru.kino.dev.vault.pres.grouplist.GroupListArgs
import ru.kino.dev.vault.pres.grouplist.GroupListProcessor
import ru.kino.dev.vault.pres.trash.TrashProcessor
import ru.kino.dev.vault.pres.trashcontainer.TrashContainerProcessor
import ru.kino.dev.vault.pres.trashentry.TrashEntryArgs
import ru.kino.dev.vault.pres.trashentry.TrashEntryProcessor

/**
 * The screens of `vault`. Several ids of one type come as one `*Args` value with named fields (pass-docs,
 * Step 84); the rest of a processor is the graph's. The navigator is the screen's - `LocalNavigator`, not the
 * graph's: inside a container it is the container's (Step 98).
 */
val vaultPresModule = module {
    // The containers of a wide window: the screens in them bring their own processors (pass-docs, Step 98, 100)
    viewModel { (dbKey: String) -> VaultContainerProcessor(dbKey, get(), get(), get()) }
    viewModel { (dbKey: String) -> TrashContainerProcessor(dbKey, get(), get()) }

    viewModel { (args: GroupListArgs, navigator: Navigator) ->
        GroupListProcessor(
            dbKey = args.dbKey,
            groupId = args.groupId,
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
            navigator = navigator,
            log = get(),
        )
    }
    viewModel { (args: EntryReadArgs, navigator: Navigator) ->
        EntryReadProcessor(
            dbKey = args.dbKey,
            entryId = args.entryId,
            get(), get(), get(), get(), get(), get(), get(), get(),
            navigator = navigator,
            log = get(),
        )
    }
    viewModel { (args: EntryEditArgs, navigator: Navigator) ->
        EntryEditProcessor(
            dbKey = args.dbKey,
            groupId = args.groupId,
            entryId = args.entryId,
            get(), get(), get(), get(), get(), get(), get(), get(),
            navigator = navigator,
            log = get(),
        )
    }
    viewModel { (args: EntryHistoryArgs, navigator: Navigator) ->
        EntryHistoryProcessor(dbKey = args.dbKey, entryId = args.entryId, get(), get(), navigator = navigator, log = get())
    }
    viewModel { (args: EntryVersionArgs, navigator: Navigator) ->
        EntryVersionProcessor(
            dbKey = args.dbKey,
            entryId = args.entryId,
            index = args.index,
            get(), get(), get(), get(),
            navigator = navigator,
            log = get(),
        )
    }
    viewModel { (dbKey: String, navigator: Navigator) ->
        TrashProcessor(dbKey, get(), get(), get(), changes = get(), navigator = navigator, log = get())
    }
    viewModel { (args: TrashEntryArgs, navigator: Navigator) ->
        TrashEntryProcessor(
            dbKey = args.dbKey,
            entryId = args.entryId,
            get(), get(), get(), get(), get(), get(),
            changes = get(),
            navigator = navigator,
            log = get(),
        )
    }
}
