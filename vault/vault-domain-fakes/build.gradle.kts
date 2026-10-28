// The fakes of vault-domain for the tests of the modules above - what `java-test-fixtures` gave before; KMP has no
// test fixtures, so a module of its own, depended on from tests only (pass-docs, Step 85)
plugins {
    id("kmp-lib")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.vault.vaultDomain)
            api(projects.core.coreDomainFakes)
        }
    }
}
