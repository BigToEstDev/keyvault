plugins {
    id("android-compose-lib")
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    implementation(projects.core.corePres)
    implementation(projects.core.coreNav)
    implementation(projects.settings.settingsDomain)
    // The password of the open database checked before it is kept for biometrics (pass-docs, Step 59)
    implementation(projects.vault.vaultDomain)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.orbit.test)
    testImplementation(projects.settings.settingsDomainFakes)
    testImplementation(projects.core.coreDomainFakes)
    testImplementation(projects.vault.vaultDomainFakes)
}
