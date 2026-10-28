package ru.kino.dev.log

/** [ErrorLog] of the platform: logcat on android, the error stream on the desktop. */
internal expect fun platformErrorLog(): ErrorLog
