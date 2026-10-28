// Top-level build file where you can add configuration options common to all sub-projects/modules.
// AGP/Kotlin/Compose plugin versions are managed by the `build-logic` convention plugins (included build).
// Applied here with `apply false` so the classpath is the root's and every module can use the plain plugin ids
plugins {
    id("kmp-lib") apply false
}

allprojects {
    group = "ru.kino.dev"
}
