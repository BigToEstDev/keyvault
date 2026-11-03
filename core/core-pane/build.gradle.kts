plugins {
    id("kmp-compose-lib")
}

/*
 * Containers of several screens side by side - the window wide enough, `Tablet`: each screen keeps its own processor,
 * the container takes the navigation that is its to handle (pass-docs, `docs/dev/navigation.md`, Step 98).
 */
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.coreNav)
            // LayoutType, the lifecycle and the viewmodels of compose
            implementation(projects.core.corePres)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
