plugins {
    id("com.android.library")
    id("kotlin-parcelize")
}

//kotlin {
//    jvmToolchain(JAVA_VERSION.majorVersion.toInt())
//    compilerOptions {
//        jvmTarget.set(JVM_TARGET)
//        freeCompilerArgs.addAll(compilerArgs)
//    }
//}

android {
    namespace = makeNamespace()

    compileSdk = COMPILE_SDK
    buildToolsVersion = BUILD_TOOLS

    defaultConfig {
        minSdk = MIN_SDK
    }

    compileOptions {
        sourceCompatibility = JAVA_VERSION
        targetCompatibility = JAVA_VERSION
    }

    buildFeatures {
        aidl = true
        resValues = false
        buildConfig = false
    }

    lint {
        checkDependencies = true
    }
}
