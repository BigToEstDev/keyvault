plugins {
    id("java-lib")
    // Fakes of the interfaces declared here, shared by the tests of the modules above
    `java-test-fixtures`
}

dependencies {
    // NativeCore, DatabaseFiles and their models are the vocabulary of this module's interfaces
    api(projects.core.coreDomain)
    // The lock listens to the timer of the settings (pass-docs, Step 59)
    api(projects.settings.settingsDomain)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(testFixtures(projects.core.coreDomain))
    testImplementation(testFixtures(projects.settings.settingsDomain))
}
