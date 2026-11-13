package ru.kino.dev.desktop.icons

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * The icon file of macOS (`.icns`): the magic `icns` and the length of the file, then elements - a type of four
 * letters, the length of the element with its 8 bytes of header, PNG. Big-endian (the default of [ByteBuffer]).
 */
internal object IcnsFile {

    /** The type of an element and the size of its picture in pixels; `@2x` - the Retina twin of a smaller one. */
    private val ELEMENTS = listOf(
        "icp4" to 16,
        "icp5" to 32,
        "ic11" to 32, // 16@2x
        "icp6" to 64,
        "ic12" to 64, // 32@2x
        "ic07" to 128,
        "ic08" to 256,
        "ic13" to 256, // 128@2x
        "ic09" to 512,
        "ic14" to 512, // 256@2x
        "ic10" to 1024, // 512@2x
    )

    /** The pictures the file needs. */
    val SIZES = ELEMENTS.map { it.second }.distinct()

    private const val MAGIC = "icns"
    private const val HEADER_SIZE = 8

    /** The file of all its elements, each taken from [pngs] - PNG by its size in pixels. */
    fun build(pngs: Map<Int, ByteArray>): ByteArray {
        val body = ByteArrayOutputStream()
        ELEMENTS.forEach { (type, size) ->
            val png = pngs.getValue(size)
            body.write(header(type, HEADER_SIZE + png.size))
            body.write(png)
        }
        return ByteArrayOutputStream().apply {
            write(header(MAGIC, HEADER_SIZE + body.size()))
            body.writeTo(this)
        }.toByteArray()
    }

    private fun header(type: String, length: Int): ByteArray =
        ByteBuffer.allocate(HEADER_SIZE).put(type.toByteArray(Charsets.US_ASCII)).putInt(length).array()
}
