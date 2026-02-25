package pro.devapp.walkietalkiek.serivce.network

import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import pro.devapp.walkietalkiek.core.flags.FeatureFlagsRepository
import pro.devapp.walkietalkiek.core.mvi.CoroutineContextProvider
import pro.devapp.walkietalkiek.core.network.MqttConfigRepository
import pro.devapp.walkietalkiek.serivce.network.data.DeviceInfoRepository
import timber.log.Timber
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

internal class MqttControlPlaneController(
    private val featureFlagsRepository: FeatureFlagsRepository,
    private val mqttConfigRepository: MqttConfigRepository,
    private val deviceInfoRepository: DeviceInfoRepository,
    coroutineContextProvider: CoroutineContextProvider
) : ControlPlaneController {

    private val lock = Any()
    private val scope = coroutineContextProvider.createScope(coroutineContextProvider.io)
    private var client: MqttAsyncClient? = null
    private var presenceJob: Job? = null
    private var selfNodeId: String? = null

    override fun start() {
        if (!featureFlagsRepository.flags.value.mqttControl) {
            Timber.Forest.i("MQTT control plane is disabled by feature flag.")
            return
        }
        synchronized(lock) {
            if (client?.isConnected == true) return
            val config = mqttConfigRepository.config.value
            val brokerUrl = "tcp://${config.brokerHost}:${config.brokerPort}"
            val deviceInfo = deviceInfoRepository.getCurrentDeviceInfo()
            selfNodeId = deviceInfo.deviceId.ifBlank { UUID.randomUUID().toString() }
            val mqttClient = MqttAsyncClient(
                brokerUrl,
                "walkie-${UUID.randomUUID()}",
                MemoryPersistence()
            )
            mqttClient.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    Timber.Forest.i("MQTT connectComplete reconnect=$reconnect server=$serverURI")
                }

                override fun connectionLost(cause: Throwable?) {
                    Timber.Forest.w(cause, "MQTT connection lost")
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    if (topic.isNullOrBlank() || message == null) return
                    handleIncomingMessage(topic, message)
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit
            })
            val options = MqttConnectOptions().apply {
                isAutomaticReconnect = true
                isCleanSession = true
                keepAliveInterval = 20
                connectionTimeout = 5
            }
            try {
                mqttClient.connect(options, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Timber.Forest.i("MQTT connected to $brokerUrl")
                        subscribePresenceAndStartHeartbeat(mqttClient)
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Timber.Forest.w(exception, "MQTT connect failed: $brokerUrl")
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
            presenceJob?.cancel()
            publishPresence(current, STATUS_OFFLINE)
            runCatching {
                if (current.isConnected) {
                    current.disconnect()
                }
                current.close()
            }.onFailure { error ->
                Timber.Forest.w(error, "MQTT stop failed")
            }
            client = null
            selfNodeId = null
        }
    }

    private fun subscribePresenceAndStartHeartbeat(mqttClient: MqttAsyncClient) {
        val presenceTopic = presenceTopic()
        runCatching {
            mqttClient.subscribe(presenceTopic, 0)
            publishPresence(mqttClient, STATUS_ONLINE)
            presenceJob?.cancel()
            presenceJob = scope.launch {
                while (isActive) {
                    delay(PRESENCE_HEARTBEAT_MS)
                    publishPresence(mqttClient, STATUS_ONLINE)
                }
            }
        }.onFailure { error ->
            Timber.Forest.w(error, "MQTT subscribe/presence setup failed")
        }
    }

    private fun publishPresence(mqttClient: MqttAsyncClient, status: String) {
        if (!mqttClient.isConnected) return
        val nodeId = selfNodeId ?: return
        val now = System.currentTimeMillis()
        val payload = "$nodeId|$status|$now".toByteArray()
        runCatching {
            mqttClient.publish(
                presenceTopic(),
                payload,
                0,
                false
            )
        }.onFailure { error ->
            Timber.Forest.w(error, "MQTT presence publish failed")
        }
    }

    private fun handleIncomingMessage(topic: String, message: MqttMessage) {
        if (topic != presenceTopic()) {
            return
        }
        val value = message.payload?.decodeToString().orEmpty()
        val tokens = value.split('|')
        if (tokens.size < 3) return
        val nodeId = tokens[0]
        if (nodeId == selfNodeId) return
        val status = tokens[1]
        val ts = tokens[2]
        Timber.Forest.i("MQTT presence: node=$nodeId status=$status ts=$ts")
    }

    private fun presenceTopic(): String {
        val clusterId = mqttConfigRepository.config.value.clusterId
        return "cluster/$clusterId/presence"
    }
}

private const val PRESENCE_HEARTBEAT_MS = 10_000L
private const val STATUS_ONLINE = "online"
private const val STATUS_OFFLINE = "offline"
