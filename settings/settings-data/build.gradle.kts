plugins {
    id("android-lib")
}

dependencies {
    api(projects.settings.settingsDomain)

    implementation(libs.hilt)
    ksp(libs.hilt.compiler)

    // The settings of the app: codes in one preferences file, kept across launches
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
