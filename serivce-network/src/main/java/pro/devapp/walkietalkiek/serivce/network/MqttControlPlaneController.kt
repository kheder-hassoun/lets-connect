package pro.devapp.walkietalkiek.serivce.network

import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import pro.devapp.walkietalkiek.core.flags.FeatureFlagsRepository
import timber.log.Timber
import java.util.UUID

internal class MqttControlPlaneController(
    private val featureFlagsRepository: FeatureFlagsRepository
) : ControlPlaneController {

    private val lock = Any()
    private var client: MqttAsyncClient? = null

    override fun start() {
        if (!featureFlagsRepository.flags.value.mqttControl) {
            Timber.Forest.i("MQTT control plane is disabled by feature flag.")
            return
        }
        synchronized(lock) {
            if (client?.isConnected == true) return
            val mqttClient = MqttAsyncClient(
                DEFAULT_BROKER_URL,
                "walkie-${UUID.randomUUID()}",
                MemoryPersistence()
            )
            val options = MqttConnectOptions().apply {
                isAutomaticReconnect = true
                isCleanSession = true
                keepAliveInterval = 20
                connectionTimeout = 5
            }
            try {
                mqttClient.connect(options, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Timber.Forest.i("MQTT connected to $DEFAULT_BROKER_URL")
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Timber.Forest.w(exception, "MQTT connect failed: $DEFAULT_BROKER_URL")
                    }
                })
                client = mqttClient
            } catch (e: Exception) {
                Timber.Forest.w(e, "MQTT start failed")
                runCatching { mqttClient.close() }
            }
        }
    }

    override fun stop() {
        synchronized(lock) {
            val current = client ?: return
            runCatching {
                if (current.isConnected) {
                    current.disconnect()
                }
                current.close()
            }.onFailure { error ->
                Timber.Forest.w(error, "MQTT stop failed")
            }
            client = null
        }
    }
}

private const val DEFAULT_BROKER_URL = "tcp://127.0.0.1:1883"
