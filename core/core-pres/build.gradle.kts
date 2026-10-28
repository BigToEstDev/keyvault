plugins {
    id("kmp-compose-lib")
}

// The strings of the whole app live here, as R.string of core-pres did: one public Res for every module, in the
// package the R class had (pass-docs, Step 87)
compose.resources {
    publicResClass = true
    packageOfResClass = "ru.kino.dev.core.pres"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.coreDomain)

            // koinViewModel and the viewModel definitions of Koin - multiplatform, for every screen
            api(libs.koin.compose.viewmodel)

            api(libs.kotlinx.coroutines.core)
            api(libs.cmp.lifecycle.viewmodel.compose)
            api(libs.cmp.lifecycle.runtime.compose)

            api(libs.orbit.core)
            api(libs.orbit.viewmodel)
            api(libs.orbit.compose)

            api(libs.cmp.runtime)
            api(libs.cmp.foundation)
            api(libs.cmp.ui)
            api(libs.cmp.material3)
            api(libs.cmp.material.icons.extended)
            api(libs.cmp.components.resources)
        }
        androidMain.dependencies {
            api(libs.kotlinx.coroutines.android)
            api(libs.androidx.activity.compose)
            // The system prompt of strong biometrics - one for welcome and settings (pass-docs, Step 59)
            implementation(libs.androidx.biometric)
        }
        getByName("androidHostTest").dependencies {
            // A dialog and a menu in the language of the app - Android on the JVM, no device (pass-docs, Step 72)
            implementation(libs.junit)
            implementation(libs.robolectric)
            implementation(libs.androidx.compose.ui.test.junit4)
        }
    }
}
