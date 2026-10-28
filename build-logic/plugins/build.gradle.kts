plugins {
    `kotlin-dsl`
//    id("org.gradle.kotlin.kotlin-dsl") version "6.5.2"
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.compose.gradle.plugin)
    implementation(libs.hilt.gradle)
    implementation(libs.ksp.gradle.plugin)
    implementation(libs.android.gradle)
    implementation(libs.android.gradle.api)
    implementation(libs.compose.gradle.plugin)
    implementation(libs.kotlin.serialization.gradle.plugin)
}
