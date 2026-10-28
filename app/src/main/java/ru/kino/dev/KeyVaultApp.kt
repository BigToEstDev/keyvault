package ru.kino.dev

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import ru.kino.dev.di.appModules
import ru.kino.dev.di.coreDataModule

class KeyVaultApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@KeyVaultApp)
            modules(appModules + coreDataModule)
        }
    }
}
