package ru.kino.dev.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import ru.kino.dev.navigation.Navigator

/** The navigator: one for the app - the screens send to it, the activity listens. */
val coreNavModule = module {
    singleOf(::Navigator)
}
