// The fakes of core-domain for the tests of every module - what `java-test-fixtures` gave before; KMP has no test
// fixtures, so a module of its own, depended on from tests only (pass-docs, Step 85)
plugins {
    id("kmp-lib")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.coreDomain)
        }
    }
}
