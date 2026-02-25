package pro.devapp.walkietalkiek.core.flags

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FeatureFlags(
    val mqttControl: Boolean = false,
    val webrtcAudio: Boolean = false,
    val centralSettings: Boolean = false,
    val floorV2: Boolean = false,
    val observabilityV2: Boolean = false
)

class FeatureFlagsRepository {
    private val _flags = MutableStateFlow(FeatureFlags())
    val flags: StateFlow<FeatureFlags> = _flags.asStateFlow()

    fun updateMqttControl(enabled: Boolean) {
        _flags.value = _flags.value.copy(mqttControl = enabled)
    }

    fun updateWebrtcAudio(enabled: Boolean) {
        _flags.value = _flags.value.copy(webrtcAudio = enabled)
    }

    fun updateCentralSettings(enabled: Boolean) {
        _flags.value = _flags.value.copy(centralSettings = enabled)
    }

    fun updateFloorV2(enabled: Boolean) {
        _flags.value = _flags.value.copy(floorV2 = enabled)
    }

    fun updateObservabilityV2(enabled: Boolean) {
        _flags.value = _flags.value.copy(observabilityV2 = enabled)
    }

    fun resetToDefaults() {
        _flags.value = FeatureFlags()
    }
}
