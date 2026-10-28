package ru.kino.dev.di

import org.koin.dsl.module
import ru.kino.dev.log.AndroidErrorLog
import ru.kino.dev.log.ErrorLog

/** What core-pres gives every screen. */
val corePresModule = module {
    single<ErrorLog> { AndroidErrorLog() }
}
