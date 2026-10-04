plugins {
    id("android-lib")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.hilt)
    ksp(libs.hilt.compiler)

    // Routes are serializable objects: the type-safe api of navigation-compose
    api(libs.kotlinx.serialization.core)
    api(libs.androidx.navigation.compose)
}
