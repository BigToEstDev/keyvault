package ru.kino.dev.desktop.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Density
import org.jetbrains.skia.EncodedImageFormat
import ru.kino.dev.ui.components.appIcon
import java.io.File

/*
 * The icons of the desktop installers, drawn from the same appIcon() as the window and the screen "About" - one source
 * of the shield, no outside tools (pass-docs, Step 124 p5). Run by `:desktopApp:exportAppIcons`; the files are kept in
 * git, jpackage takes them from there. The shield changes - run it again.
 */

/** The base of the names of the icon files: keyvault.ico, keyvault.icns, keyvault.png. */
private const val FILE_NAME = "keyvault"

/** The icon of Linux - one picture. */
private const val LINUX_SIZE = 512

/** @param args the directory the icons are written to */
fun main(args: Array<String>) {
    val outDir = File(requireNotNull(args.singleOrNull()) { "Usage: IconExport <output directory>" })
    outDir.mkdirs()

    val sizes = (IcoFile.SIZES + IcnsFile.SIZES + LINUX_SIZE).toSortedSet()
    val pngs = sizes.associateWith(::renderPng)

    outDir.resolve("$FILE_NAME.ico").writeBytes(IcoFile.build(pngs))
    outDir.resolve("$FILE_NAME.icns").writeBytes(IcnsFile.build(pngs))
    outDir.resolve("$FILE_NAME.png").writeBytes(pngs.getValue(LINUX_SIZE))
    println("App icons written to $outDir")
}

/** appIcon() drawn on a transparent square of [size] pixels, as PNG. */
private fun renderPng(size: Int): ByteArray {
    val scene = ImageComposeScene(width = size, height = size, density = Density(1f)) {
        Image(painter = rememberVectorPainter(appIcon()), contentDescription = null, modifier = Modifier.fillMaxSize())
    }
    try {
        val data = requireNotNull(scene.render().encodeToData(EncodedImageFormat.PNG)) { "PNG of $size px not encoded" }
        return data.bytes
    } finally {
        scene.close()
    }
}
