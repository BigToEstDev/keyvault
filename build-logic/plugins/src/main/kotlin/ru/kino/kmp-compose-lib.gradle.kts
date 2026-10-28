/**
 * A multiplatform library with Compose UI: [kmp-lib] plus Compose Multiplatform and its resources.
 */
plugins {
    id("kmp-lib")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    android {
        androidResources {
            enable = true
        }
    }
}
