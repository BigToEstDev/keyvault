plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    // Builds rust/pass-ffi for the host and puts the library into the app resources
    id("rust-bridge-desktop")
}

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
    implementation(projects.settings.settingsDomain)
    implementation(libs.koin.core)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
}

compose.desktop {
    application {
        mainClass = "ru.kino.dev.desktop.MainKt"

        // Installers and their settings - Step 89 cmp (pass-docs)
        nativeDistributions {
            packageName = "KeyVault"
            packageVersion = "0.1.0"
        }
    }
}
