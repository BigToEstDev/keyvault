/**
 * The app on every platform: its root - `App()`, the navigation of all features - and its graph (pass-docs,
 * Step 88). The android app (`app`) and the desktop one (`desktopApp`) only start it: AGP 9 holds no application in a
 * multiplatform module.
 */
plugins {
    id("kmp-compose-lib")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.corePres)
            implementation(projects.core.coreNav)
            // The data modules: the graph puts them together, nothing here calls them
            implementation(projects.core.coreData)
            implementation(projects.welcome.welcomeData)
            implementation(projects.vault.vaultData)
            implementation(projects.settings.settingsData)
            implementation(projects.welcome.welcomePres)
            implementation(projects.vault.vaultPres)
            implementation(projects.settings.settingsPres)
            // The theme, the language and what the screens show are settings: the root reads them
            implementation(projects.settings.settingsDomain)
        }
    }
}
