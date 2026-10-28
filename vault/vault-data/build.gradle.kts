plugins {
    id("kmp-lib")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.vault.vaultDomain)
            // The directory of the app and its preferences stores (pass-docs, Step 86)
            implementation(projects.core.coreData)

            implementation(libs.koin.core)

            // The filter of the list: two codes in preferences, kept across launches
            implementation(libs.androidx.datastore.preferences)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(projects.core.coreDomainFakes)
        }
    }
}
