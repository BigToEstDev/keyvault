plugins {
    id("kmp-compose-lib")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.corePres)
            implementation(projects.core.coreNav)
            implementation(projects.welcome.welcomeDomain)
            // The sign-in settings: welcome listens through the domain of settings (pass-docs, Step 59)
            implementation(projects.settings.settingsDomain)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.orbit.test)
            implementation(projects.settings.settingsDomainFakes)
        }
    }
}
