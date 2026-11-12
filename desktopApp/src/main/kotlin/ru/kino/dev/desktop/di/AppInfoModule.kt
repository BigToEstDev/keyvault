package ru.kino.dev.desktop.di

import org.koin.dsl.module
import ru.kino.dev.settings.about.AppInfo
import ru.kino.dev.settings.about.BuildType

/**
 * What the desktop app tells of itself on the screen "About" (pass-docs, Step 122): the properties the build puts on
 * its launch (`desktopApp/build.gradle.kts`). A release is the app installed: the launcher of jpackage names its path;
 * run from Gradle - a debug build, its version marked "-debug" as on android (pass-docs, Step 123).
 */
val appInfoModule = module {
    single {
        val buildType = if (System.getProperty(JPACKAGE_APP_PATH) != null) BuildType.Release else BuildType.Debug
        AppInfo(
            version = property(VERSION) + if (buildType == BuildType.Debug) DEBUG_SUFFIX else "",
            buildType = buildType,
            repoUrl = property(REPO_URL),
            contactEmail = property(CONTACT_EMAIL),
        )
    }
}

/** A property the build always sets: without it the launch is broken - better seen at once. */
private fun property(name: String): String =
    checkNotNull(System.getProperty(name)) { "The launch of the app has no -D$name" }

private const val VERSION = "app.version"
private const val REPO_URL = "app.repoUrl"
private const val CONTACT_EMAIL = "app.contactEmail"

/** What the version of a debug build ends with - as `versionNameSuffix` of android. */
private const val DEBUG_SUFFIX = "-debug"

/** Set by the launcher of an app packaged with jpackage. */
private const val JPACKAGE_APP_PATH = "jpackage.app-path"
