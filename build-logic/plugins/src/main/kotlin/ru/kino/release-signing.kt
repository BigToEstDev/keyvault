import org.gradle.api.Project
import java.io.File
import java.util.Properties

/*
 * The key the android release is signed with (pass-docs, docs/dev/apk-signing.md, Step 124). Kept outside git: locally
 * in `keystore.properties` at the root of the project, in CI - in the environment. Neither is there - the release is
 * built unsigned, the build goes on.
 */

/** The key store and the key in it the release is signed with. */
class ReleaseSigning(
    val storeFile: File,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String,
)

/** The names of the key: in `keystore.properties` and, prefixed with `KEYVAULT_SIGNING_`, in the environment. */
private enum class SigningKey(val property: String, val variable: String) {
    STORE_FILE("storeFile", "KEYVAULT_SIGNING_STORE_FILE"),
    STORE_PASSWORD("storePassword", "KEYVAULT_SIGNING_STORE_PASSWORD"),
    KEY_ALIAS("keyAlias", "KEYVAULT_SIGNING_KEY_ALIAS"),
    KEY_PASSWORD("keyPassword", "KEYVAULT_SIGNING_KEY_PASSWORD"),
}

/**
 * The key of the release: `keystore.properties` at the root, else the `KEYVAULT_SIGNING_*` variables; null - neither
 * is there. Read as inputs of the configuration: a change reconfigures. Half a key (some values missing, the store file
 * not found) fails the build - a release must not come out unsigned by a typo.
 */
fun Project.releaseSigning(): ReleaseSigning? {
    val file = rootProject.layout.projectDirectory.file("keystore.properties")
    val text = providers.fileContents(file).asText.orNull
    val (source, values) = if (text != null) {
        val properties = Properties().apply { load(text.reader()) }
        "keystore.properties" to SigningKey.entries.associateWith { properties.getProperty(it.property) }
    } else {
        "KEYVAULT_SIGNING_*" to SigningKey.entries.associateWith { providers.environmentVariable(it.variable).orNull }
    }
    val present = values.filterValues { !it.isNullOrBlank() }.mapValues { it.value!!.trim() }
    if (present.isEmpty()) return null

    val missing = SigningKey.entries - present.keys
    require(missing.isEmpty()) {
        "$source: no ${missing.joinToString { if (text != null) it.property else it.variable }} - " +
            "the release signing key is given only in part"
    }
    val storeFile = rootProject.file(present.getValue(SigningKey.STORE_FILE))
    require(storeFile.isFile) { "$source: the key store $storeFile is not found" }
    return ReleaseSigning(
        storeFile = storeFile,
        storePassword = present.getValue(SigningKey.STORE_PASSWORD),
        keyAlias = present.getValue(SigningKey.KEY_ALIAS),
        keyPassword = present.getValue(SigningKey.KEY_PASSWORD),
    )
}
