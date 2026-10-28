/**
 * A multiplatform library without UI: android and desktop (jvm) targets, `commonMain` for the shared
 * code. The desktop target is named `desktop`, so its sources live in `desktopMain`.
 *
 * Android goes through `com.android.kotlin.multiplatform.library`: AGP 9 has no android target of its own
 * in a KMP module otherwise. The modules move onto it one by one (pass-docs, Step 85-88 cmp); until then
 * the `android-*` plugins stay next to it.
 */
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    android {
        namespace = makeNamespace()
        compileSdk = COMPILE_SDK
        minSdk = MIN_SDK
        compilerOptions {
            jvmTarget.set(JVM_TARGET)
        }
        // commonTest runs on android too, as host tests on the jvm of the machine (androidHostTest). With the
        // resources of the module: Robolectric tests of core-pres read its strings (pass-docs, Step 72, 87)
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JVM_TARGET)
        }
    }

    jvmToolchain(JAVA_VERSION.majorVersion.toInt())

    compilerOptions {
        freeCompilerArgs.addAll(compilerArgs)
    }

    sourceSets {
        // Every module tests with kotlin.test - the version is the one of the Kotlin plugin
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
