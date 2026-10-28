plugins {
    id("kmp-lib")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)

            // Routes are serializable objects: the type-safe api of navigation-compose - multiplatform (pass-docs,
            // Step 88)
            api(libs.kotlinx.serialization.core)
            api(libs.cmp.navigation.compose)
        }
    }
}
