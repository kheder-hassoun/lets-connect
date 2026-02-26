package pro.devapp.walkietalkiek.serivce.network

import android.util.Base64
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
import pro.devapp.walkietalkiek.serivce.network.data.PttFloorRepository
import pro.devapp.walkietalkiek.serivce.network.data.TextMessagesRepository
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
    private val pttFloorRepository: PttFloorRepository,
    private val textMessagesRepository: TextMessagesRepository,
    coroutineContextProvider: CoroutineContextProvider
) : ControlPlaneController, ChatPublisher, FloorPublisher {

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

    override fun publishChatMessage(message: String): Boolean {
        if (!featureFlagsRepository.flags.value.mqttControl) return false
        val mqttClient = synchronized(lock) { client } ?: return false
        if (!mqttClient.isConnected) return false
        val nodeId = selfNodeId ?: return false
        val payload = encodeChatPayload(nodeId, message)
        return runCatching {
            mqttClient.publish(chatTopic(), payload, 0, false)
            true
        }.getOrElse { error ->
            Timber.Forest.w(error, "MQTT chat publish failed")
            false
        }
    }

    override fun publishAcquire(): Boolean {
        return publishFloorCommand(FLOOR_ACQUIRE)
    }

    override fun publishRelease(): Boolean {
        return publishFloorCommand(FLOOR_RELEASE)
    }

    private fun subscribePresenceAndStartHeartbeat(mqttClient: MqttAsyncClient) {
        val presenceTopic = presenceTopic()
        val chatTopic = chatTopic()
        val floorTopic = floorTopic()
        runCatching {
            mqttClient.subscribe(presenceTopic, 0)
            mqttClient.subscribe(chatTopic, 0)
            mqttClient.subscribe(floorTopic, 0)
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
        when (topic) {
            presenceTopic() -> handlePresenceMessage(message)
            chatTopic() -> handleChatMessage(message)
            floorTopic() -> handleFloorMessage(message)
            else -> Unit
        }
    }

    private fun handlePresenceMessage(message: MqttMessage) {
        val value = message.payload?.decodeToString().orEmpty()
        val tokens = value.split('|')
        if (tokens.size < 3) return
        val nodeId = tokens[0]
        if (nodeId == selfNodeId) return
        val status = tokens[1]
        val ts = tokens[2]
        Timber.Forest.i("MQTT presence: node=$nodeId status=$status ts=$ts")
    }

    private fun handleChatMessage(message: MqttMessage) {
        val (nodeId, content) = decodeChatPayload(message.payload) ?: return
        if (nodeId == selfNodeId) return
        textMessagesRepository.addMessage(
            message = content,
            hostAddress = MQTT_HOST_PREFIX + nodeId
        )
    }

    private fun handleFloorMessage(message: MqttMessage) {
        val (nodeId, command) = decodeFloorPayload(message.payload) ?: return
        val owner = MQTT_HOST_PREFIX + nodeId
        when (command) {
            FLOOR_ACQUIRE -> pttFloorRepository.acquire(owner)
            FLOOR_RELEASE -> pttFloorRepository.release(owner)
            else -> Unit
        }
    }

    private fun presenceTopic(): String {
        val clusterId = mqttConfigRepository.config.value.clusterId
        return "cluster/$clusterId/presence"
    }

    private fun chatTopic(): String {
        val clusterId = mqttConfigRepository.config.value.clusterId
        return "cluster/$clusterId/chat"
    }

    private fun floorTopic(): String {
        val clusterId = mqttConfigRepository.config.value.clusterId
        return "cluster/$clusterId/floor"
    }

    private fun encodeChatPayload(nodeId: String, message: String): ByteArray {
        val messageEncoded = Base64.encodeToString(
            message.toByteArray(),
            Base64.NO_WRAP or Base64.NO_PADDING
        )
        return "$nodeId|$messageEncoded".toByteArray()
    }

    private fun decodeChatPayload(payload: ByteArray?): Pair<String, String>? {
        if (payload == null || payload.isEmpty()) return null
        val value = payload.decodeToString()
        val tokens = value.split('|', limit = 2)
        if (tokens.size < 2) return null
        val nodeId = tokens[0]
        if (nodeId.isBlank()) return null
        val messageBytes = runCatching {
            Base64.decode(tokens[1], Base64.NO_WRAP or Base64.NO_PADDING)
        }.getOrNull() ?: return null
        val message = messageBytes.decodeToString().trim()
        if (message.isEmpty()) return null
        return nodeId to message
    }

    private fun publishFloorCommand(command: String): Boolean {
        if (!featureFlagsRepository.flags.value.mqttControl) return false
        val mqttClient = synchronized(lock) { client } ?: return false
        if (!mqttClient.isConnected) return false
        val nodeId = selfNodeId ?: return false
        val payload = "$nodeId|$command|${System.currentTimeMillis()}".toByteArray()
        return runCatching {
            mqttClient.publish(floorTopic(), payload, 0, false)
            true
        }.getOrElse { error ->
            Timber.Forest.w(error, "MQTT floor publish failed")
            false
        }
    }

    private fun decodeFloorPayload(payload: ByteArray?): Pair<String, String>? {
        if (payload == null || payload.isEmpty()) return null
        val value = payload.decodeToString()
        val tokens = value.split('|')
        if (tokens.size < 2) return null
        val nodeId = tokens[0]
        val command = tokens[1]
        if (nodeId.isBlank()) return null
        if (command != FLOOR_ACQUIRE && command != FLOOR_RELEASE) return null
        return nodeId to command
    }
}

private const val PRESENCE_HEARTBEAT_MS = 10_000L
private const val STATUS_ONLINE = "online"
private const val STATUS_OFFLINE = "offline"
private const val MQTT_HOST_PREFIX = "mqtt:"
private const val FLOOR_ACQUIRE = "ACQUIRE"
private const val FLOOR_RELEASE = "RELEASE"
