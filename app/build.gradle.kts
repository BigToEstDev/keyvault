plugins {
    id("android-compose-app")
    // Builds rust/pass-ffi with cargo-ndk and puts the .so into src/main/jniLibs
    id("rust-bridge-android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "ru.kino.dev"

    defaultConfig {
        applicationId = "ru.kino.dev"
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // The language is chosen in the app, not only by the system: every language goes into the base, or a
    // bundle from the store would bring only the language of the system (pass-docs, Step 57)
    bundle {
        language {
            enableSplit = false
        }
    }

//    buildTypes {
//        release {
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
//        }
//    }
}

dependencies {
    implementation(libs.hilt)
    ksp(libs.hilt.compiler)
    compileOnly(libs.errorprone.annotations)
    // The system splash stays up while the start route is read
    implementation(libs.androidx.core.splashscreen)
    // FragmentActivity for BiometricPrompt
    implementation(libs.androidx.biometric)

    implementation(projects.core.coreDomain)
    implementation(projects.core.corePres)
    implementation(projects.core.coreNav)
    // The native core: its Hilt module binds NativeCore to the jni implementation
    implementation(projects.core.coreData)
    // Feature data modules: nothing here calls them, but Hilt only sees the bindings of what the app depends on
    implementation(projects.welcome.welcomeData)
    implementation(projects.vault.vaultData)
    implementation(projects.settings.settingsData)
    implementation(projects.welcome.welcomePres)
    implementation(projects.vault.vaultPres)
    implementation(projects.settings.settingsPres)
    // The theme of the whole app is a setting: the activity reads it
    implementation(projects.settings.settingsDomain)
}