package ru.kino.dev.desktop.icons

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The icon file of Windows (`.ico`): a header, a directory of the pictures, then the pictures themselves as PNG
 * (taken by Windows since Vista). Little-endian.
 */
internal object IcoFile {

    /** The pictures Windows picks from: the taskbar, the explorer at every scale, the installer. */
    val SIZES = listOf(16, 24, 32, 48, 64, 128, 256)

    private const val HEADER_SIZE = 6
    private const val ENTRY_SIZE = 16
    private const val TYPE_ICON: Short = 1
    private const val PLANES: Short = 1
    private const val BITS_PER_PIXEL: Short = 32

    /** The file of [SIZES], each taken from [pngs] - PNG by its size in pixels. */
    fun build(pngs: Map<Int, ByteArray>): ByteArray {
        val images = SIZES.map { pngs.getValue(it) }
        val header = ByteBuffer.allocate(HEADER_SIZE + ENTRY_SIZE * SIZES.size).order(ByteOrder.LITTLE_ENDIAN)
        header.putShort(0).putShort(TYPE_ICON).putShort(SIZES.size.toShort())

        var offset = header.capacity()
        SIZES.zip(images).forEach { (size, png) ->
            // One byte for the side: 256 is written as 0
            val side = (size % 256).toByte()
            header.put(side).put(side).put(0).put(0)
            header.putShort(PLANES).putShort(BITS_PER_PIXEL)
            header.putInt(png.size).putInt(offset)
            offset += png.size
        }

        return ByteArrayOutputStream(offset).apply {
            write(header.array())
            images.forEach(::write)
        }.toByteArray()
    }
}
