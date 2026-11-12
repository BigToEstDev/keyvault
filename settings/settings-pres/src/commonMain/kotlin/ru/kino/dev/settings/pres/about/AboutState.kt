package ru.kino.dev.settings.pres.about

import ru.kino.dev.settings.about.AppInfo
import ru.kino.dev.settings.about.BuildType

/**
 * What the screen "About" shows (pass-docs, Step 122) - known when the screen opens, nothing to wait for.
 *
 * @param version the version of the build
 * @param buildType a debug build or one for a release
 * @param email the address to write to
 * @param repoUrl the repository of the app
 */
data class AboutState(
    val version: String,
    val buildType: BuildType,
    val email: String,
    val repoUrl: String,
) {

    /** The repository as it reads - with no scheme: `github.com/…`. */
    val repoShown: String = repoUrl.substringAfter("://").trimEnd('/')

    companion object {

        fun of(info: AppInfo) = AboutState(
            version = info.version,
            buildType = info.buildType,
            email = info.contactEmail,
            repoUrl = info.repoUrl,
        )
    }
}
