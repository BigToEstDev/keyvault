plugins {
    id("kmp-compose-lib")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.corePres)
            implementation(projects.core.coreNav)
            implementation(projects.vault.vaultDomain)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.orbit.test)
            implementation(projects.vault.vaultDomainFakes)
            implementation(projects.core.coreDomainFakes)
            implementation(projects.settings.settingsDomainFakes)
        }
    }
}
