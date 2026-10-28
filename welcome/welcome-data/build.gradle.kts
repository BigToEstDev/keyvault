plugins {
    id("android-lib")
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    api(projects.welcome.welcomeDomain)
    // Biometrics are of settings: a database remembered anew forgets the stale ones (pass-docs, Step 59)
    implementation(projects.settings.settingsDomain)

    implementation(libs.koin.android)

    // The remembered database: one json string in preferences, the whole schema
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(projects.core.coreDomainFakes)
}
