package ru.kino.dev.di

import org.koin.dsl.module
import ru.kino.dev.BuildConfig
import ru.kino.dev.settings.about.AppInfo
import ru.kino.dev.settings.about.BuildType

/** What the android app tells of itself on the screen "About" - from its `BuildConfig` (pass-docs, Step 122). */
val appInfoModule = module {
    single {
        AppInfo(
            version = BuildConfig.VERSION_NAME,
            buildType = if (BuildConfig.DEBUG) BuildType.Debug else BuildType.Release,
            repoUrl = BuildConfig.REPO_URL,
            contactEmail = BuildConfig.CONTACT_EMAIL,
        )
    }
}
