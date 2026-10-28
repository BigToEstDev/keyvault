plugins {
    id("android-compose-lib")
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    implementation(projects.core.corePres)
    implementation(projects.core.coreNav)
    implementation(projects.vault.vaultDomain)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.orbit.test)
    testImplementation(testFixtures(projects.vault.vaultDomain))
    testImplementation(testFixtures(projects.core.coreDomain))
    testImplementation(testFixtures(projects.settings.settingsDomain))
}
