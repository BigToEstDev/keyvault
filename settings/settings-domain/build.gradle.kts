plugins {
    id("java-lib")
    // Fakes of the interfaces declared here, shared by the tests of the modules that listen
    `java-test-fixtures`
}

dependencies {
    // The settings are read as flows
    api(libs.kotlinx.coroutines.core)
}
