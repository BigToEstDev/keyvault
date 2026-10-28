package ru.kino.dev.core

import java.io.File
import java.nio.file.Files

/**
 * A directory of a test's own, for the files of stores - what the `TemporaryFolder` rule of JUnit gave, for
 * kotlin.test: made at once, gone with everything in it on [delete], which the test calls in `@AfterTest`
 * (pass-docs, Step 86). On the jvm of both platforms, as the stores under test are.
 */
class TemporaryFolder {

    val root: File = Files.createTempDirectory("pass-test").toFile()

    private var folders = 0

    /** A fresh directory inside [root]. */
    fun newFolder(): File = File(root, "folder-${folders++}").apply { mkdirs() }

    fun delete() {
        root.deleteRecursively()
    }
}
