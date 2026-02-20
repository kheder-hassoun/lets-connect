package pro.devapp.walkietalkiek.core.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettingsRepository {
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun updateTalkDurationSeconds(seconds: Int) {
        val normalized = seconds.coerceIn(5, 120)
        _settings.value = _settings.value.copy(
            talkDurationSeconds = normalized
        )
    }

    fun updateToneEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(
            toneEnabled = enabled
        )
    }
}

data class AppSettings(
    val talkDurationSeconds: Int = 10,
    val toneEnabled: Boolean = true
)
