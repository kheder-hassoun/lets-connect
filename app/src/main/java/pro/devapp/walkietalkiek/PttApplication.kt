package pro.devapp.walkietalkiek

import android.app.Application
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import pro.devapp.walkietalkiek.di.appModule
import pro.devapp.walkietalkiek.core.diagnostics.DeviceFileLogTree
import pro.devapp.walkietalkiek.core.diagnostics.DeviceLogStore
import pro.devapp.walkietalkiek.core.settings.AppSettingsRepository
import pro.devapp.walkietalkiek.localization.AppLocaleManager
import timber.log.Timber

class PttApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            allowOverride(true)
            androidContext(applicationContext)
            modules(
                appModule
            )
            // Uncomment to add koin logs
            // androidLogger(Level.DEBUG)
        }

        val appSettingsRepository: AppSettingsRepository = get()
        AppLocaleManager.applyLanguage(
            context = this,
            language = appSettingsRepository.settings.value.appLanguage
        )

        val deviceLogStore: DeviceLogStore = get()
        installCrashCapture(deviceLogStore)
        Timber.plant(DeviceFileLogTree(deviceLogStore))
    }

    private fun installCrashCapture(deviceLogStore: DeviceLogStore) {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            deviceLogStore.writeCrash(thread.name, throwable)
            previousHandler?.uncaughtException(thread, throwable)
                ?: run { throw throwable }
        }
    }
}
