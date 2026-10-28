plugins {
    id("kmp-lib")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // CredentialLimits for the password field, and the vocabulary of the core under the interfaces
            api(projects.core.coreDomain)
        }
    }
}
