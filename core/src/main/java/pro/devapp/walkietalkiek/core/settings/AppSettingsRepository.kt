package pro.devapp.walkietalkiek.core.settings

import android.content.Context
import android.content.res.Resources
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettingsRepository(
    context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    init {
        // Persist first-launch auto language choice so app locale is stable across restarts.
        if (!prefs.contains(KEY_APP_LANGUAGE)) {
            persist(_settings.value)
        }
    }

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

    fun updateThemeMode(themeMode: ThemeMode) {
        val updated = _settings.value.copy(
            themeMode = themeMode
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

    fun updateShowWelcomeTutorial(show: Boolean) {
        val updated = _settings.value.copy(
            showWelcomeTutorial = show
        )
        _settings.value = updated
        persist(updated)
    }

    fun resetToDefaults() {
        val defaults = AppSettings(
            appLanguage = resolveDefaultLanguage(),
            showWelcomeTutorial = true
        )
        _settings.value = defaults
        persist(defaults)
    }

    private fun loadSettings(): AppSettings {
        val talkDuration = prefs.getInt(KEY_TALK_DURATION, 10).coerceIn(5, 120)
        val toneEnabled = prefs.getBoolean(KEY_TONE_ENABLED, true)
        val themeName = prefs.getString(KEY_THEME_COLOR, ThemeColor.ORANGE.name)
        val themeModeName = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        val showWelcomeTutorial = prefs.getBoolean(KEY_SHOW_WELCOME_TUTORIAL, true)
        val languageCode = if (prefs.contains(KEY_APP_LANGUAGE)) {
            prefs.getString(KEY_APP_LANGUAGE, AppLanguage.ENGLISH.code)
        } else {
            resolveDefaultLanguage().code
        }
        return AppSettings(
            talkDurationSeconds = talkDuration,
            toneEnabled = toneEnabled,
            themeColor = ThemeColor.entries.firstOrNull { it.name == themeName } ?: ThemeColor.ORANGE,
            themeMode = ThemeMode.entries.firstOrNull { it.name == themeModeName } ?: ThemeMode.SYSTEM,
            appLanguage = AppLanguage.entries.firstOrNull { it.code == languageCode } ?: AppLanguage.ENGLISH,
            showWelcomeTutorial = showWelcomeTutorial
        )
    }

    private fun resolveDefaultLanguage(): AppLanguage {
        val deviceLanguage = Resources.getSystem()
            .configuration
            .locales
            .get(0)
            ?.language
            ?.lowercase()
            .orEmpty()

        return if (deviceLanguage == AppLanguage.ARABIC.code) {
            AppLanguage.ARABIC
        } else {
            AppLanguage.ENGLISH
        }
    }

    private fun persist(settings: AppSettings) {
        prefs.edit()
            .putInt(KEY_TALK_DURATION, settings.talkDurationSeconds)
            .putBoolean(KEY_TONE_ENABLED, settings.toneEnabled)
            .putString(KEY_THEME_COLOR, settings.themeColor.name)
            .putString(KEY_THEME_MODE, settings.themeMode.name)
            .putString(KEY_APP_LANGUAGE, settings.appLanguage.code)
            .putBoolean(KEY_SHOW_WELCOME_TUTORIAL, settings.showWelcomeTutorial)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "app_settings"
        const val KEY_TALK_DURATION = "talk_duration_seconds"
        const val KEY_TONE_ENABLED = "tone_enabled"
        const val KEY_THEME_COLOR = "theme_color"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_APP_LANGUAGE = "app_language"
        const val KEY_SHOW_WELCOME_TUTORIAL = "show_welcome_tutorial"
    }
}

data class AppSettings(
    val talkDurationSeconds: Int = 10,
    val toneEnabled: Boolean = true,
    val themeColor: ThemeColor = ThemeColor.ORANGE,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.ENGLISH,
    val showWelcomeTutorial: Boolean = true
)

enum class ThemeColor(val title: String) {
    ORANGE("Orange"),
    PURPLE("Purple"),
    BLUE("Blue")
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AppLanguage(
    val code: String
) {
    ENGLISH("en"),
    ARABIC("ar")
}
