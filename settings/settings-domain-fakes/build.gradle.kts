// The fakes of settings-domain for the tests of the modules that listen - what `java-test-fixtures` gave before; KMP
// has no test fixtures, so a module of its own, depended on from tests only (pass-docs, Step 85)
plugins {
    id("kmp-lib")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.settings.settingsDomain)
        }
    }
}
