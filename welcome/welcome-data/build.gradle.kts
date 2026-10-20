plugins {
    id("android-lib")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(projects.welcome.welcomeDomain)
    // Biometrics are of settings: a database remembered anew forgets the stale ones (pass-docs, Step 59)
    implementation(projects.settings.settingsDomain)

    implementation(libs.hilt)
    ksp(libs.hilt.compiler)

    // The remembered database: one json string in preferences, the whole schema
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(testFixtures(projects.core.coreDomain))
}
