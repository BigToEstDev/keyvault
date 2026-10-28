plugins {
    id("kmp-lib")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // NativeCore, DatabaseFiles and their models are the vocabulary of this module's interfaces
            api(projects.core.coreDomain)
            // The lock listens to the timer of the settings (pass-docs, Step 59)
            api(projects.settings.settingsDomain)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(projects.vault.vaultDomainFakes)
            implementation(projects.settings.settingsDomainFakes)
        }
    }
}
