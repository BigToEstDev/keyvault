plugins {
    id("android-lib")
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    implementation(libs.koin.core)

    // Routes are serializable objects: the type-safe api of navigation-compose
    api(libs.kotlinx.serialization.core)
    api(libs.androidx.navigation.compose)
}
