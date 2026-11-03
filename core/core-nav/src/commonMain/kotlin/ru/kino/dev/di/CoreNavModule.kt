package ru.kino.dev.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import ru.kino.dev.navigation.AppNavigator

/**
 * The navigator of the app: one - `AppNavHost` listens, `App()` gives it to the screens as `LocalNavigator`. Not bound
 * as `Navigator`: a processor takes the navigator of its screen, never the graph's (pass-docs, Step 98).
 */
val coreNavModule = module {
    singleOf(::AppNavigator)
}
