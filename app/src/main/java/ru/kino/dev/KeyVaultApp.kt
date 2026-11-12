package ru.kino.dev

import android.app.Application
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import ru.kino.dev.core.SharedFiles
import ru.kino.dev.di.appInfoModule
import ru.kino.dev.di.appModules
import ru.kino.dev.di.coreDataModule

class KeyVaultApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val koin = startKoin {
            androidContext(this@KeyVaultApp)
            modules(appModules + coreDataModule + appInfoModule)
        }.koin
        // Files sent before: when the other app was done reading them is never told (pass-docs, Step 111)
        val shared = koin.get<SharedFiles>()
        MainScope().launch { shared.clear() }
    }
}
