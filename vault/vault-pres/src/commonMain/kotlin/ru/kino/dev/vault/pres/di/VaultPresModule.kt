package ru.kino.dev.vault.pres.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
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
import ru.kino.dev.vault.pres.trashentry.TrashEntryArgs
import ru.kino.dev.vault.pres.trashentry.TrashEntryProcessor

/**
 * The screens of `vault`. Several ids of one type come as one `*Args` value with named fields (pass-docs,
 * Step 84); the rest of a processor is the graph's.
 */
val vaultPresModule = module {
    viewModel { (args: GroupListArgs) ->
        GroupListProcessor(
            dbKey = args.dbKey,
            groupId = args.groupId,
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
        )
    }
    viewModel { (args: EntryReadArgs) ->
        EntryReadProcessor(
            dbKey = args.dbKey,
            entryId = args.entryId,
            get(), get(), get(), get(), get(), get(), get(), get(), get(),
        )
    }
    viewModel { (args: EntryEditArgs) ->
        EntryEditProcessor(
            dbKey = args.dbKey,
            groupId = args.groupId,
            entryId = args.entryId,
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
        )
    }
    viewModel { (args: EntryHistoryArgs) ->
        EntryHistoryProcessor(dbKey = args.dbKey, entryId = args.entryId, get(), get(), get(), get())
    }
    viewModel { (args: EntryVersionArgs) ->
        EntryVersionProcessor(
            dbKey = args.dbKey,
            entryId = args.entryId,
            index = args.index,
            get(), get(), get(), get(), get(), get(),
        )
    }
    viewModel { (dbKey: String) -> TrashProcessor(dbKey, get(), get(), get(), get(), get()) }
    viewModel { (args: TrashEntryArgs) ->
        TrashEntryProcessor(
            dbKey = args.dbKey,
            entryId = args.entryId,
            get(), get(), get(), get(), get(), get(), get(), get(),
        )
    }
}
