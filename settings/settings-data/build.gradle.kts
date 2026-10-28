plugins {
    id("android-lib")
    // The record of the biometric password is a json string (pass-docs, Step 33)
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    api(projects.settings.settingsDomain)

    implementation(libs.hilt)
    ksp(libs.hilt.compiler)

    // The settings of the app: codes in one preferences file, kept across launches
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    // Strong biometrics: BiometricManager - moved here from welcome (pass-docs, Step 59)
    implementation(libs.androidx.biometric)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(testFixtures(projects.core.coreDomain))
}
