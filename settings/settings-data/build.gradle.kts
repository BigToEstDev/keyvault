plugins {
    id("kmp-lib")
    // The record of the biometric password is a json string (pass-docs, Step 33)
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.settings.settingsDomain)
            // The directory of the app and its preferences stores (pass-docs, Step 86)
            implementation(projects.core.coreData)

            implementation(libs.koin.core)

            // The settings of the app: codes in one preferences file, kept across launches
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            // androidContext for the Keystore
            implementation(libs.koin.android)
            // Strong biometrics: BiometricManager - moved here from welcome (pass-docs, Step 59)
            implementation(libs.androidx.biometric)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(projects.core.coreDomainFakes)
        }
    }
}
