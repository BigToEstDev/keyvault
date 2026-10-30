package ru.kino.dev.desktop

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary
import java.awt.Window

/**
 * The title bar of the window in the theme of the system (pass-docs, Step 93) - dark with a dark system, no colors
 * of the app. Windows draws it light whatever the theme unless told; macOS follows the system once
 * [followSystemOnMac] is set; on Linux it is the window manager's, nothing to do.
 */
internal object TitleBar {

    private val windows = System.getProperty("os.name").startsWith("Windows")

    /** Before any window: the title bars of macOS take the appearance of the system. */
    fun followSystemOnMac() {
        System.setProperty("apple.awt.application.appearance", "system")
    }

    /** The title bar of [window] on Windows dark or light; elsewhere nothing. */
    fun applyDark(window: Window, dark: Boolean) {
        if (!windows) return
        val hwnd = HWND(Native.getWindowPointer(window) ?: return)
        val value = IntByReference(if (dark) 1 else 0)
        // Windows 10 before 20H1 knows the attribute by its older number
        val result = dwm.DwmSetWindowAttribute(hwnd, DARK_MODE, value.pointer, Int.SIZE_BYTES)
        if (result != S_OK) dwm.DwmSetWindowAttribute(hwnd, DARK_MODE_BEFORE_20H1, value.pointer, Int.SIZE_BYTES)
    }

    @Suppress("FunctionName")
    private interface Dwmapi : StdCallLibrary {
        fun DwmSetWindowAttribute(hwnd: HWND, attribute: Int, value: Pointer, size: Int): Int
    }

    private val dwm: Dwmapi by lazy { Native.load("dwmapi", Dwmapi::class.java) }

    // DWMWA_USE_IMMERSIVE_DARK_MODE
    private const val DARK_MODE = 20
    private const val DARK_MODE_BEFORE_20H1 = 19
    private const val S_OK = 0
}
