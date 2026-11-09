plugins {
    id("kmp-lib")
    // @Serializable of the ffi envelope and its dto-s needs the compiler plugin here, not only in domain
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    // Reapplied explicitly: the dependsOn() below would otherwise switch the default template off
    // (kotlinlang.org, "Hierarchical project structure" - "Creating additional source sets")
    applyDefaultHierarchyTemplate()

    sourceSets {
        // Code of both jvm targets: the jni bridge and libphonenumber (Java) are the same on android and on the
        // desktop jvm, only loading the native library differs (androidMain / desktopMain). Kept apart from
        // commonMain so that commonMain stays free of the jvm (pass-docs, Step 82, the rule for iOS)
        val jvmSharedMain by creating {
            dependsOn(commonMain.get())
        }
        androidMain.get().dependsOn(jvmSharedMain)
        getByName("desktopMain").dependsOn(jvmSharedMain)

        val jvmSharedTest by creating {
            dependsOn(commonTest.get())
        }
        getByName("androidHostTest").dependsOn(jvmSharedTest)
        getByName("desktopTest").dependsOn(jvmSharedTest)

        commonMain.dependencies {
            api(projects.core.coreDomain)
            api(libs.kotlinx.coroutines.core)

            // The graph of the app
            implementation(libs.koin.core)

            // The presets of one's own and the phone prefix: preferences stores of the app (pass-docs, Step 86)
            api(libs.androidx.datastore.preferences)
        }
        jvmSharedMain.dependencies {
            // Phone numbers of every country, offline: the field of the type "phone" (pass-docs, Step 77)
            implementation(libs.libphonenumber)
        }
        androidMain.dependencies {
            api(libs.kotlinx.coroutines.android)
            // androidContext for the storage access framework and filesDir
            implementation(libs.koin.android)
            // String.toUri for the uris the storage access framework hands around
            implementation(libs.androidx.core.ktx)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(projects.core.coreDomainFakes)
        }
        getByName("androidHostTest").dependencies {
            // A provider of documents of the test's own behind a real ContentResolver - Android on the JVM, no
            // device (pass-docs, Step 112)
            implementation(libs.junit)
            implementation(libs.robolectric)
        }
    }
}
