/**
 * Builds the Rust bridge (`rust/pass-ffi`) for the host and puts the library into the app resources of
 * the desktop app (`compose.desktop.application.appResourcesRootDir`), from where both `run` and the
 * installers pick it up. The app loads it from `compose.application.resources.dir`.
 *
 * Always a release build: the debug core runs the key derivation many times slower, and a desktop user
 * never gets a debug core anyway. Cargo writes into its own `--target-dir` under build/, apart from the
 * android build of the same crate (`rust-bridge-android`).
 */

val crateDir = rootProject.layout.projectDirectory.dir("rust/pass-ffi").asFile
val coreDir = rootProject.file("../pass-rust-core")

// The resources layout of compose desktop: <root>/<os>-<arch>/ is picked for the current platform
val hostDir = run {
    val os = System.getProperty("os.name").lowercase()
    val osName = when {
        os.contains("win") -> "windows"
        os.contains("mac") -> "macos"
        else -> "linux"
    }
    val arch = if (System.getProperty("os.arch").contains("aarch64")) "arm64" else "x64"
    "$osName-$arch"
}

val nativeResourcesDir = layout.buildDirectory.dir("rustNativeResources")

val cargoBuildHost = tasks.register<Exec>("cargoBuildHost") {
    group = "rust"
    description = "Builds pass-ffi for the host ($hostDir, release)"

    val targetDir = rootProject.layout.buildDirectory.dir("cargo-host").get().asFile
    val outDir = nativeResourcesDir.get().dir(hostDir).asFile
    // Locals only below: a task action that touches the script object breaks the configuration cache
    val libraryName = System.mapLibraryName("pass_ffi")
    val builtLibrary = targetDir.resolve("release/$libraryName")
    val copiedLibrary = outDir.resolve(libraryName)

    workingDir = crateDir

    // Rebuilt when the crate or the core changes; the core is a path dependency, so cargo sees it too
    inputs.dir(crateDir.resolve("src"))
    inputs.file(crateDir.resolve("Cargo.toml"))
    inputs.file(crateDir.resolve("build.rs"))
    inputs.dir(coreDir.resolve("crates"))
    inputs.file(coreDir.resolve("Cargo.toml"))
    outputs.dir(outDir)

    commandLine("cargo", "build", "--release", "--target-dir", targetDir.absolutePath)

    doLast {
        outDir.mkdirs()
        builtLibrary.copyTo(copiedLibrary, overwrite = true)
    }
}

plugins.withId("org.jetbrains.compose") {
    val compose = extensions.getByType<org.jetbrains.compose.ComposeExtension>()
    compose.extensions.configure<org.jetbrains.compose.desktop.DesktopExtension>("desktop") {
        application.nativeDistributions.appResourcesRootDir.set(nativeResourcesDir)
    }
}

// prepareAppResources copies appResourcesRootDir for run and for the installers alike
tasks.matching { it.name == "prepareAppResources" }.configureEach { dependsOn(cargoBuildHost) }
