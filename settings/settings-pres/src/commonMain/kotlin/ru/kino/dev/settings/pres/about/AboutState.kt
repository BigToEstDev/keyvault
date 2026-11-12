package ru.kino.dev.settings.pres.about

import ru.kino.dev.settings.about.AppInfo
import ru.kino.dev.settings.about.BuildType

/**
 * What the screen "About" shows (pass-docs, Step 122): the app is known when the screen opens; the version of the
 * core is asked of the core (Step 123).
 *
 * @param version the version of the build
 * @param buildType a debug build or one for a release
 * @param email the address to write to
 * @param repoUrl the repository of the app
 * @param coreVersion the version of the core, as far as it is known
 */
data class AboutState(
    val version: String,
    val buildType: BuildType,
    val email: String,
    val repoUrl: String,
    val coreVersion: CoreVersionShown = CoreVersionShown.Loading,
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

/** The version of the core on the screen. */
sealed interface CoreVersionShown {

    /** Asked, not answered yet - a skeleton in its place. */
    data object Loading : CoreVersionShown

    data class Known(val version: String) : CoreVersionShown

    /** The core did not answer: the line says the version is unknown. */
    data object Unknown : CoreVersionShown
}
