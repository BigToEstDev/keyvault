package ru.kino.dev.settings.about

/**
 * What the app tells of itself on the screen "About" (pass-docs, Step 122). Given by the platform: the build knows the
 * version and its type - `BuildConfig` on android, the launcher on the desktop; the addresses come from
 * `gradle.properties`, one place for both.
 *
 * @param version the version of the build, as the person sees it
 * @param buildType a debug build or one for a release
 * @param repoUrl the public repository of the app - not of the core
 * @param contactEmail where to write
 */
data class AppInfo(
    val version: String,
    val buildType: BuildType,
    val repoUrl: String,
    val contactEmail: String,
) {

    /** Where bugs are reported - the issues of the repository of the app. */
    val issuesUrl: String = "${repoUrl.trimEnd('/')}/issues"
}

/** The type of the build. */
enum class BuildType { Debug, Release }
