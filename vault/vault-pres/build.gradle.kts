plugins {
    id("android-compose-lib")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.core.corePres)
    implementation(projects.core.coreNav)
    implementation(projects.vault.vaultDomain)

    implementation(libs.hilt)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.orbit.test)
    testImplementation(testFixtures(projects.vault.vaultDomain))
}
