package pro.devapp.walkietalkiek.core.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettingsRepository(
    context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun updateTalkDurationSeconds(seconds: Int) {
        val normalized = seconds.coerceIn(5, 120)
        val updated = _settings.value.copy(
            talkDurationSeconds = normalized
        )
        _settings.value = updated
        persist(updated)
    }

    fun updateToneEnabled(enabled: Boolean) {
        val updated = _settings.value.copy(
            toneEnabled = enabled
        )
        _settings.value = updated
        persist(updated)
    }

    fun updateThemeColor(themeColor: ThemeColor) {
        val updated = _settings.value.copy(
            themeColor = themeColor
        )
        _settings.value = updated
        persist(updated)
    }

    fun updateAppLanguage(appLanguage: AppLanguage) {
        val updated = _settings.value.copy(
            appLanguage = appLanguage
        )
        _settings.value = updated
        persist(updated)
    }

    fun resetToDefaults() {
        val defaults = AppSettings()
        _settings.value = defaults
        persist(defaults)
    }

    private fun loadSettings(): AppSettings {
        val talkDuration = prefs.getInt(KEY_TALK_DURATION, 10).coerceIn(5, 120)
        val toneEnabled = prefs.getBoolean(KEY_TONE_ENABLED, true)
        val themeName = prefs.getString(KEY_THEME_COLOR, ThemeColor.PURPLE.name)
        val languageCode = prefs.getString(KEY_APP_LANGUAGE, AppLanguage.ENGLISH.code)
        return AppSettings(
            talkDurationSeconds = talkDuration,
            toneEnabled = toneEnabled,
            themeColor = ThemeColor.entries.firstOrNull { it.name == themeName } ?: ThemeColor.PURPLE,
            appLanguage = AppLanguage.entries.firstOrNull { it.code == languageCode } ?: AppLanguage.ENGLISH
        )
    }

    private fun persist(settings: AppSettings) {
        prefs.edit()
            .putInt(KEY_TALK_DURATION, settings.talkDurationSeconds)
            .putBoolean(KEY_TONE_ENABLED, settings.toneEnabled)
            .putString(KEY_THEME_COLOR, settings.themeColor.name)
            .putString(KEY_APP_LANGUAGE, settings.appLanguage.code)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "app_settings"
        const val KEY_TALK_DURATION = "talk_duration_seconds"
        const val KEY_TONE_ENABLED = "tone_enabled"
        const val KEY_THEME_COLOR = "theme_color"
        const val KEY_APP_LANGUAGE = "app_language"
    }
}

data class AppSettings(
    val talkDurationSeconds: Int = 10,
    val toneEnabled: Boolean = true,
    val themeColor: ThemeColor = ThemeColor.PURPLE,
    val appLanguage: AppLanguage = AppLanguage.ENGLISH
)

enum class ThemeColor(val title: String) {
    PURPLE("Purple"),
    BLUE("Blue")
}

enum class AppLanguage(
    val code: String
) {
    ENGLISH("en"),
    ARABIC("ar")
}
