plugins {
    id("android-compose-lib")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.core.corePres)
    implementation(projects.core.coreNav)
    implementation(projects.settings.settingsDomain)
    // The password of the open database checked before it is kept for biometrics (pass-docs, Step 59)
    implementation(projects.vault.vaultDomain)

    implementation(libs.hilt)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.orbit.test)
    testImplementation(testFixtures(projects.settings.settingsDomain))
    testImplementation(testFixtures(projects.core.coreDomain))
    testImplementation(testFixtures(projects.vault.vaultDomain))
}
