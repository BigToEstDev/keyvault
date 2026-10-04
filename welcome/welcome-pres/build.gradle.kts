plugins {
    id("android-compose-lib")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.core.corePres)
    implementation(projects.core.coreNav)
    implementation(projects.welcome.welcomeDomain)

    implementation(libs.hilt)
    // Strong biometrics: BiometricManager, BiometricPrompt
    implementation(libs.androidx.biometric)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.orbit.test)
}
