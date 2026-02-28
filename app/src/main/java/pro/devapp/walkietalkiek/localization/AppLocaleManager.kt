package pro.devapp.walkietalkiek.localization

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import pro.devapp.walkietalkiek.core.settings.AppLanguage
import java.util.Locale

object AppLocaleManager {

    @SuppressLint("AppBundleLocaleChanges")
    fun applyLanguage(
        context: Context,
        language: AppLanguage
    ): Boolean {
        val desiredLocale = Locale.forLanguageTag(language.code)
        val currentLocale = context.resources.configuration.locales.get(0)
        if (currentLocale?.language == desiredLocale.language) {
            return false
        }

        Locale.setDefault(desiredLocale)
        val updatedConfig = Configuration(context.resources.configuration).apply {
            setLocale(desiredLocale)
            setLayoutDirection(desiredLocale)
        }
        context.resources.updateConfiguration(updatedConfig, context.resources.displayMetrics)
        context.applicationContext.resources.updateConfiguration(
            updatedConfig,
            context.applicationContext.resources.displayMetrics
        )

        val locales = LocaleListCompat.forLanguageTags(language.code)
        AppCompatDelegate.setApplicationLocales(locales)
        return true
    }
}
