package ru.kino.dev.log

import android.util.Log
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/** [ErrorLog] into logcat. */
internal class AndroidErrorLog @Inject constructor() : ErrorLog {

    override fun unexpected(where: String, error: Throwable) {
        Log.e(where, "unexpected failure", error)
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal interface ErrorLogModule {

    @Binds
    @Singleton
    fun bind(log: AndroidErrorLog): ErrorLog
}
