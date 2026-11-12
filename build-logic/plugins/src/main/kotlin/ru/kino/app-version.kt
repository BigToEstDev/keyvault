import groovy.json.JsonSlurper
import org.gradle.api.Project

/*
 * The version of the app - one for android and the desktop, kept in `version.json` at the root of the project
 * (pass-docs: Step 123, docs/dev/versions-rules.md). The build reads it here; CI reads the same file with `jq`.
 */

/** What `version.json` names: the [name] of the files of the builds and the [version] of the app. */
class AppArtifact(val name: String, val version: AppVersion) {

    /** The base of the name of a build: `keyvault-1.0.0-rc.1`. */
    val fileName: String get() = "$name-${version.name}"
}

/**
 * A version of the app, SemVer: `MAJOR.MINOR.PATCH`, a release candidate - `-rc.N`.
 *
 * @param rc the number of the release candidate; null - the release itself
 */
class AppVersion(val major: Int, val minor: Int, val patch: Int, val rc: Int?) {

    /** As it is written and shown: `1.0.0-rc.1`, `1.2.3`. */
    val name: String get() = "$major.$minor.$patch" + (rc?.let { "-rc.$it" } ?: "")

    /**
     * The version code of android - grows with every version: two digits for `MINOR`, `PATCH` and the candidate, the
     * release over all its candidates (`99`): `1.0.0-rc.1` - `1000001`, `1.0.0` - `1000099`, `1.2.3` - `1020399`.
     */
    val code: Int get() = major * 1_000_000 + minor * 10_000 + patch * 100 + (rc ?: RELEASE)

    /** The version of the installers of the desktop: jpackage takes digits only - `-rc.N` is in the file name. */
    val packageVersion: String get() = "$major.$minor.$patch"

    companion object {

        /** The last two digits of [code] of a release: above every candidate. */
        private const val RELEASE = 99

        private val FORMAT = Regex("""(\d+)\.(\d+)\.(\d+)(?:-rc\.(\d+))?""")

        /**
         * Reads [text] - a version that breaks [code] or the installers fails the build: `MAJOR` from 1 (the
         * installers of macOS take no 0) to 2099 (the code of android stops at 2100000000), `MINOR` and `PATCH` below
         * 100, a candidate from 1 to 98.
         */
        fun parse(text: String): AppVersion {
            val match = requireNotNull(FORMAT.matchEntire(text)) {
                "version.json: \"$text\" is not MAJOR.MINOR.PATCH or MAJOR.MINOR.PATCH-rc.N"
            }
            val (major, minor, patch) = match.destructured.toList().take(3).map(String::toInt)
            val rc = match.groupValues[4].takeIf(String::isNotEmpty)?.toInt()
            require(major in 1..2099) { "version.json: MAJOR $major is not within 1..2099" }
            require(minor in 0..99 && patch in 0..99) { "version.json: MINOR and PATCH of $text must be below 100" }
            require(rc == null || rc in 1..98) { "version.json: the candidate of $text is not within rc.1..rc.98" }
            return AppVersion(major, minor, patch, rc)
        }
    }
}

/** The artifact of `version.json` at the root - read as an input of the configuration: a change reconfigures. */
fun Project.appArtifact(): AppArtifact {
    val file = rootProject.layout.projectDirectory.file("version.json")
    val text = providers.fileContents(file).asText.get()
    val artifact = requireNotNull((JsonSlurper().parseText(text) as? Map<*, *>)?.get("artifact") as? Map<*, *>) {
        "version.json: no \"artifact\" object"
    }
    val name = requireNotNull(artifact["name"] as? String) { "version.json: no artifact.name" }
    val version = requireNotNull(artifact["version"] as? String) { "version.json: no artifact.version" }
    return AppArtifact(name, AppVersion.parse(version))
}
