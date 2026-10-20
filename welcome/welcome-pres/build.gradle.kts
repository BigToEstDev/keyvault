plugins {
    id("android-compose-lib")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.core.corePres)
    implementation(projects.core.coreNav)
    implementation(projects.welcome.welcomeDomain)
    // The sign-in settings: welcome listens through the domain of settings (pass-docs, Step 59)
    implementation(projects.settings.settingsDomain)

    implementation(libs.hilt)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.orbit.test)
    testImplementation(testFixtures(projects.settings.settingsDomain))
}
