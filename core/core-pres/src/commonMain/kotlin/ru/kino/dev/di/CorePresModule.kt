package ru.kino.dev.di

import org.koin.dsl.module
import ru.kino.dev.log.ErrorLog
import ru.kino.dev.log.platformErrorLog

/** What core-pres gives every screen. */
val corePresModule = module {
    single<ErrorLog> { platformErrorLog() }
}
