import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    // Builds rust/pass-ffi for the host and puts the library into the app resources
    id("rust-bridge-desktop")
}

/** The name and the version of the app - version.json at the root, one for android and the desktop (Step 123). */
val artifact = appArtifact()

/** Who publishes the app - in the installers (pass-docs, Step 124). */
val VENDOR = "BigToEstDev"

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

// The icons of the installers drawn from appIcon() - a tool of the build, not shipped in the app (pass-docs, Step 124)
val iconExport by sourceSets.creating

dependencies {
    "iconExportImplementation"(projects.core.corePres)
    "iconExportImplementation"(compose.desktop.currentOs)
}

tasks.register<JavaExec>("exportAppIcons") {
    group = "distribution"
    description = "Draws appIcon() into icons/keyvault.ico, .icns and .png - the icons of the installers"
    classpath = iconExport.runtimeClasspath
    mainClass.set("ru.kino.dev.desktop.icons.IconExportKt")
    args(layout.projectDirectory.dir("icons").asFile.absolutePath)
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

        // The installers (pass-docs, Step 124): jpackage builds only those of the OS it runs on - CI builds each on its
        // own runner. No license dialog: the GPL is not to be accepted to use the app
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Dmg)
            packageName = "KeyVault"
            // Digits only - the installers take no "-rc.N"; it is in the name of the file, given in CI
            packageVersion = artifact.version.packageVersion
            description = "KeePass (KDBX) password manager"
            vendor = VENDOR
            copyright = "Copyright (C) 2026 $VENDOR. GPL-3.0-only"
            // The modules of Java the trimmed runtime of the installer carries on top of the defaults of Compose - by
            // suggestRuntimeModules; a missing one fails only the installed app, not run or the tests. A new library -
            // run it again
            modules("java.instrument", "jdk.security.auth", "jdk.unsupported")

            val icons = layout.projectDirectory.dir("icons")
            windows {
                iconFile.set(icons.file("keyvault.ico"))
                // Never to be changed: by it an installer of a new version upgrades the installed app, not sits beside
                upgradeUuid = "94f00f7a-00b7-4b4d-bdce-211c2300f554"
                // Installed as an ordinary app of the current user - no administrator rights
                perUserInstall = true
                menu = true
                menuGroup = "KeyVault"
                shortcut = true
            }
            macOS {
                iconFile.set(icons.file("keyvault.icns"))
                // The same as the applicationId of android
                bundleID = "io.github.bigtoestdev.keyvault"
            }
            linux {
                iconFile.set(icons.file("keyvault.png"))
                packageName = "keyvault"
                appCategory = "Utility"
                debMaintainer = "$VENDOR <${providers.gradleProperty("app.contactEmail").get()}>"
            }
        }

        // The release installers (packageRelease*) go through ProGuard: shrunk and optimized, names kept - Compose does
        // not obfuscate by default. The rules of the app - next to this file
        buildTypes.release.proguard {
            configurationFiles.from(layout.projectDirectory.file("proguard-rules.pro"))
        }
    }
}

// The msi asks which shortcuts to make - on the desktop, in the Start menu (pass-docs, Step 124 p7). The DSL of Compose
// has no such option, so it is handed to jpackage as is; only to the msi tasks - jpackage of other OSes refuses it
tasks.withType<AbstractJPackageTask>()
    .matching { it.name == "packageMsi" || it.name == "packageReleaseMsi" }
    .configureEach { freeArgs.add("--win-shortcut-prompt") }
