plugins {
    id("kmp-compose-lib")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.corePres)
            implementation(projects.core.coreNav)
            implementation(projects.core.corePane)
            implementation(projects.settings.settingsDomain)
            // The password of the open database checked before it is kept for biometrics (pass-docs, Step 59)
            implementation(projects.vault.vaultDomain)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.orbit.test)
            implementation(projects.settings.settingsDomainFakes)
            implementation(projects.core.coreDomainFakes)
            implementation(projects.vault.vaultDomainFakes)
        }
    }
}
