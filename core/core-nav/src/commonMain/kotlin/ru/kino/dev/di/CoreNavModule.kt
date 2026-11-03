package ru.kino.dev.di

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import ru.kino.dev.navigation.AppNavigator
import ru.kino.dev.navigation.Navigator

/** The navigator: one for the app - the screens send to it, `AppNavHost` listens. */
val coreNavModule = module {
    singleOf(::AppNavigator) { bind<Navigator>() }
}
