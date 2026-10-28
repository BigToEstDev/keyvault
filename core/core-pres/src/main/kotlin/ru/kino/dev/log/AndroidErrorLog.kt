package ru.kino.dev.log

import android.util.Log

/** [ErrorLog] into logcat. */
internal class AndroidErrorLog : ErrorLog {

    override fun unexpected(where: String, error: Throwable) {
        Log.e(where, "unexpected failure", error)
    }
}
