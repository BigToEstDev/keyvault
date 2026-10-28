plugins {
    id("android-lib")
}

dependencies {
    api(projects.vault.vaultDomain)

    implementation(libs.koin.android)

    // The filter of the list: two codes in preferences, kept across launches
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(projects.core.coreDomainFakes)
}
