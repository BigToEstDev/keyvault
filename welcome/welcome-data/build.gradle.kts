plugins {
    id("kmp-lib")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.welcome.welcomeDomain)
            // Biometrics are of settings: a database remembered anew forgets the stale ones (pass-docs, Step 59)
            implementation(projects.settings.settingsDomain)
            // The directory of the app and its preferences stores (pass-docs, Step 86)
            implementation(projects.core.coreData)

            implementation(libs.koin.core)

            // The remembered database: one json string in preferences, the whole schema
            implementation(libs.androidx.datastore.preferences)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(projects.core.coreDomainFakes)
        }
    }
}
