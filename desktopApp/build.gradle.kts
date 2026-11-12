plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    // Builds rust/pass-ffi for the host and puts the library into the app resources
    id("rust-bridge-desktop")
}

/** The name and the version of the app - version.json at the root, one for android and the desktop (Step 123). */
val artifact = appArtifact()

kotlin {
    jvmToolchain(JAVA_VERSION.majorVersion.toInt())
    compilerOptions {
        jvmTarget.set(JVM_TARGET)
        freeCompilerArgs.addAll(compilerArgs)
    }
}

dependencies {
    // The app common to the platforms, and the core of the jvm its graph lacks (pass-docs, Step 88)
    implementation(projects.composeApp)
    implementation(projects.core.coreData)
    implementation(projects.core.corePres)
    implementation(projects.settings.settingsDomain)
    // Told the window is shown again - the quiet check of the sync listens (pass-docs, Step 108)
    implementation(projects.vault.vaultDomain)
    implementation(libs.koin.core)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
    // The title bar of Windows in the theme of the system (pass-docs, Step 93)
    implementation(libs.jna.platform)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(projects.settings.settingsDomainFakes)
}

compose.desktop {
    application {
        mainClass = "ru.kino.dev.desktop.MainKt"

        // What the app tells of itself on the screen "About" (pass-docs, Step 122): the version - version.json, the
        // addresses - gradle.properties, one place for both platforms. Read by AppInfoModule
        jvmArgs += listOf(
            "-Dapp.version=${artifact.version.name}",
            "-Dapp.repoUrl=${providers.gradleProperty("app.repoUrl").get()}",
            "-Dapp.contactEmail=${providers.gradleProperty("app.contactEmail").get()}",
        )

        // Installers and their settings - the build for a release, not planned yet (pass-docs)
        nativeDistributions {
            packageName = "KeyVault"
            // Digits only - the installers take no "-rc.N"; it is in the name of the file, given in CI
            packageVersion = artifact.version.packageVersion
        }
    }
}
