plugins {
    id("kmp-lib")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // The settings are read as flows
            api(libs.kotlinx.coroutines.core)
        }
    }
}
