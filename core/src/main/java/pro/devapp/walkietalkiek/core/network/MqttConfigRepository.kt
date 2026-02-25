package pro.devapp.walkietalkiek.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MqttConfig(
    val brokerHost: String = "127.0.0.1",
    val brokerPort: Int = 1883,
    val clusterId: String = "venue-dev-01"
)

class MqttConfigRepository {
    private val _config = MutableStateFlow(MqttConfig())
    val config: StateFlow<MqttConfig> = _config.asStateFlow()

    fun updateBrokerHost(host: String) {
        val normalized = host.trim().ifBlank { "127.0.0.1" }
        _config.value = _config.value.copy(brokerHost = normalized)
    }

    fun updateBrokerPort(portText: String) {
        val parsed = portText.trim().toIntOrNull() ?: return
        val normalized = parsed.coerceIn(1, 65535)
        _config.value = _config.value.copy(brokerPort = normalized)
    }

    fun updateClusterId(clusterId: String) {
        val normalized = clusterId.trim().ifBlank { "venue-dev-01" }
        _config.value = _config.value.copy(clusterId = normalized)
    }
}
