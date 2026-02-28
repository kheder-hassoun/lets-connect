package pro.devapp.walkietalkiek.serivce.network

import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import pro.devapp.walkietalkiek.core.mvi.CoroutineContextProvider
import pro.devapp.walkietalkiek.serivce.network.data.ClusterMembershipRepository
import pro.devapp.walkietalkiek.serivce.network.data.ConnectedDevicesRepository
import pro.devapp.walkietalkiek.serivce.network.data.PttFloorRepository
import pro.devapp.walkietalkiek.serivce.network.data.TextMessagesRepository
import timber.log.Timber
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit

class SocketServer(
    private val connectedDevicesRepository: ConnectedDevicesRepository,
    private val clientSocket: SocketClient,
    private val floorArbitrationState: FloorArbitrationState,
    private val clusterMembershipRepository: ClusterMembershipRepository,
    private val textMessagesRepository: TextMessagesRepository,
    private val pttFloorRepository: PttFloorRepository,
    private val coroutineContextProvider: CoroutineContextProvider
) {

    private val acceptConnectionScope = coroutineContextProvider.createScope(
        coroutineContextProvider.io
    )

    private val readDataScope = coroutineContextProvider.createScope(
        coroutineContextProvider.io
    )

    private val writeDataScope = coroutineContextProvider.createScope(
        coroutineContextProvider.io
    )
    private var floorOwnerMonitorJob: Job? = null

    @Volatile
    private var serverPort: Int? = null

    /**
     * Data for sending
     */
    private val outputQueueMap = ConcurrentHashMap<String, LinkedBlockingDeque<ByteArray>>()

    private var socket: ServerSocket? = null

    var dataListener: ((bytes: ByteArray) -> Unit)? = null

    fun initServer(): Int {
        startFloorOwnerMonitorIfNeeded()
        if (socket != null && socket?.isClosed == false) {
            return serverPort ?: DEFAULT_SERVER_PORT
        }
        val (createdSocket, selectedPort) = createServerSocket()
        socket = createdSocket.apply {
            reuseAddress = false
            // Avoid blocking forever in accept loop.
            soTimeout = 5000
        }
        serverPort = selectedPort
        Timber.Forest.i("Server port initialized: $selectedPort")
        acceptConnectionScope.launch {
            delay(100L)
            while (acceptConnectionScope.isActive) {
                try {
                    val client = socket!!.accept()
                    client.sendBufferSize = 8192
                    client.receiveBufferSize = 8192 * 2
                    client.tcpNoDelay = true
                    val hostAddress = client.inetAddress.hostAddress.orEmpty()
                    outputQueueMap[hostAddress] = LinkedBlockingDeque()
                    connectedDevicesRepository.addOrUpdateHostStateToConnected(hostAddress, client.port)
                    handleConnection(client)
                } catch (e: Exception) {
                    if (e !is java.net.SocketTimeoutException) {
                        Timber.Forest.w("Error accepting connection: ${e.message}")
                    }
                }
                delay(1000L)
            }
        }
        return selectedPort
    }

    private fun createServerSocket(): Pair<ServerSocket, Int> {
        val preferredPort = DEFAULT_SERVER_PORT
        val fallbackPorts = (preferredPort + 1)..(preferredPort + PORT_FALLBACK_SPAN)
        val candidatePorts = sequenceOf(preferredPort) + fallbackPorts.asSequence()
        var lastError: Exception? = null
        candidatePorts.forEach { port ->
            try {
                return ServerSocket(port) to port
            } catch (error: Exception) {
                lastError = error
                Timber.Forest.w(error, "Failed to bind server socket on port=%d", port)
            }
        }
        throw IllegalStateException(
            "Unable to bind server socket in range $preferredPort..${preferredPort + PORT_FALLBACK_SPAN}",
            lastError
        )
    }

    private fun startFloorOwnerMonitorIfNeeded() {
        if (floorOwnerMonitorJob?.isActive == true) return
        floorOwnerMonitorJob = acceptConnectionScope.launch {
            var previousOwner: String? = pttFloorRepository.currentFloorOwnerHost.value
            pttFloorRepository.currentFloorOwnerHost.collect { currentOwner ->
                val ownerReleased = previousOwner != null && currentOwner == null
                previousOwner = currentOwner
                if (!ownerReleased) {
                    return@collect
                }
                val status = clusterMembershipRepository.status.value
                if (status.role == pro.devapp.walkietalkiek.serivce.network.data.ClusterRole.LEADER) {
                    Timber.Forest.i("Floor owner released/expired. Attempting queued grant.")
                    grantNextQueuedIfAny()
                }
            }
        }
    }

    private fun handleConnection(client: Socket) {
        val hostAddress = client.inetAddress?.hostAddress
        if (hostAddress.isNullOrBlank()) {
            Timber.Forest.w("Client host address is null/blank. Closing client socket.")
            client.close()
            return
        }
        var errorCounter = 0
        val readingFuture = readDataScope.launch {
            val dataInput = DataInputStream(client.getInputStream())
            Timber.Forest.i("Started reading $hostAddress")
            try {
                while (!client.isClosed && !client.isInputShutdown) {
                    val packetSize = dataInput.readInt()
                    if (packetSize <= 0 || packetSize > MAX_PACKET_SIZE) {
                        Timber.Forest.w("Skip invalid packet size: $packetSize")
                        continue
                    }
                    val data = ByteArray(packetSize)
                    dataInput.readFully(data)
                    read(data, hostAddress)
                }
            } catch (e: Exception) {
                Timber.Forest.w(e)
            } finally {
                closeClient(client)
            }
        }
        writeDataScope.launch {
            if (!client.isClosed) {
                val outputStream = DataOutputStream(client.getOutputStream())
                while (!client.isClosed && !client.isOutputShutdown) {
                    try {
                        val buf =
                            if (outputQueueMap[hostAddress]?.isEmpty() == true) {
                                outputQueueMap[hostAddress]?.pollFirst(
                                    250,
                                    TimeUnit.MILLISECONDS
                                )
                            } else {
                                outputQueueMap[hostAddress]?.pollFirst()
                            }
                        buf?.let { payload ->
                            outputStream.writeInt(payload.size)
                            outputStream.write(payload)
                            outputStream.flush()
                        }
                        errorCounter = 0
                    } catch (e: Exception) {
                        errorCounter++
                        if (errorCounter > 3) {
                            Timber.Forest.d("errorCounter $errorCounter")
                            readingFuture.cancel()
                            closeClient(client)
                        }
                    }
                }
            }
        }
    }

    private fun closeClient(client: Socket) {
        val hostAddress = client.inetAddress?.hostAddress ?: return
        client.close()
        outputQueueMap.remove(hostAddress)
        val removedNodeIds = floorArbitrationState.removeHost(hostAddress)
        if (removedNodeIds.isNotEmpty()) {
            removedNodeIds.forEach { nodeId ->
                pttFloorRepository.releaseIfOwner("node:$nodeId")
            }
            val status = clusterMembershipRepository.status.value
            if (status.role == pro.devapp.walkietalkiek.serivce.network.data.ClusterRole.LEADER &&
                pttFloorRepository.currentFloorOwnerHost.value == null
            ) {
                grantNextQueuedIfAny()
            }
        }
        clientSocket.removeClient(hostAddress)
        connectedDevicesRepository.setHostDisconnected(hostAddress)
    }

    private fun read(data: ByteArray, hostAddress: String) {
        if (data.isNotEmpty() && data[0] == AUDIO_PACKET_PREFIX) {
            val audioPayload = data.copyOfRange(1, data.size)
            if (audioPayload.isNotEmpty()) {
                dataListener?.invoke(audioPayload)
            }
        } else {
            val message = String(data).trim()
            Timber.Forest.i("message: $message from $hostAddress")
            val clusterControl = ServerlessControlProtocol.parse(message)
            if (clusterControl != null) {
                when (clusterControl) {
                    is ControlEnvelope.Heartbeat -> {
                        clusterMembershipRepository.onHeartbeat(
                            nodeId = clusterControl.nodeId,
                            term = clusterControl.term,
                            timestampMs = clusterControl.timestampMs,
                            nowMs = System.currentTimeMillis(),
                            joinedAtMs = clusterControl.startedAtMs,
                            uptimeMs = clusterControl.uptimeMs
                        )
                        floorArbitrationState.rememberNodeHost(clusterControl.nodeId, hostAddress)
                    }
                    is ControlEnvelope.FloorRequest -> {
                        clusterMembershipRepository.onHeartbeat(
                            nodeId = clusterControl.nodeId,
                            term = clusterControl.term,
                            timestampMs = clusterControl.timestampMs,
                            nowMs = System.currentTimeMillis()
                        )
                        floorArbitrationState.rememberNodeHost(clusterControl.nodeId, hostAddress)
                        handleFloorRequestAsLeader(clusterControl, hostAddress)
                    }
                    is ControlEnvelope.FloorGrant -> {
                        Timber.Forest.i("FLOOR_GRANT received target=${clusterControl.targetNodeId}")
                        pttFloorRepository.acquire("node:${clusterControl.targetNodeId}")
                    }
                    is ControlEnvelope.FloorRelease -> {
                        Timber.Forest.i("FLOOR_RELEASE received node=${clusterControl.nodeId}")
                        floorArbitrationState.removeNodeFromQueue(clusterControl.nodeId)
                        val released = pttFloorRepository.releaseIfOwner("node:${clusterControl.nodeId}") ||
                            pttFloorRepository.releaseByNodeId(clusterControl.nodeId)
                        val status = clusterMembershipRepository.status.value
                        if (status.role == pro.devapp.walkietalkiek.serivce.network.data.ClusterRole.LEADER &&
                            (released || pttFloorRepository.currentFloorOwnerHost.value == null)
                        ) {
                            grantNextQueuedIfAny()
                        }
                    }
                    is ControlEnvelope.FloorBusy -> {
                        Timber.Forest.i("FLOOR_BUSY received owner=${clusterControl.ownerNodeId}")
                        pttFloorRepository.acquire("node:${clusterControl.ownerNodeId}")
                    }
                }
            } else {
                val controlCommand = FloorControlProtocol.parse(message)
                if (controlCommand != null) {
                when (controlCommand) {
                    FloorControlCommand.Acquire -> pttFloorRepository.acquire(hostAddress)
                    FloorControlCommand.Release -> pttFloorRepository.release(hostAddress)
                }
                } else if (message == "ping"){
                    clientSocket.sendMessageToHost(
                        hostAddress = hostAddress,
                        data = "pong".toByteArray()
                    )
                } else if (message != "pong" && message.isNotEmpty()) {
                    textMessagesRepository.addMessage(
                        message = message,
                        hostAddress = hostAddress
                    )
                }
            }
        }
        connectedDevicesRepository.storeDataReceivedTime(hostAddress)
    }

    private fun handleFloorRequestAsLeader(request: ControlEnvelope.FloorRequest, requesterHostAddress: String) {
        val status = clusterMembershipRepository.status.value
        if (status.role != pro.devapp.walkietalkiek.serivce.network.data.ClusterRole.LEADER) return
        val currentOwner = pttFloorRepository.currentFloorOwnerHost.value
        val requestedOwner = "node:${request.nodeId}"
        val shouldGrant = currentOwner == null || currentOwner == requestedOwner
        if (shouldGrant) {
            Timber.Forest.i("Granting floor to ${request.nodeId} host=$requesterHostAddress")
            floorArbitrationState.removeNodeFromQueue(request.nodeId)
            pttFloorRepository.acquire(requestedOwner)
            val grant = ServerlessControlProtocol.floorGrantPacket(
                leaderNodeId = status.selfNodeId,
                targetNodeId = request.nodeId,
                term = status.term,
                seq = clusterMembershipRepository.nextSequence(),
                timestampMs = System.currentTimeMillis()
            )
            sendControlEnvelope(grant, requesterHostAddress)
        } else {
            floorArbitrationState.enqueue(request.nodeId)
            Timber.Forest.i("Queueing floor request from ${request.nodeId}; owner=$currentOwner")
            val ownerNodeId = currentOwner?.removePrefix("node:")?.ifBlank { status.selfNodeId } ?: status.selfNodeId
            val busy = ServerlessControlProtocol.floorBusyPacket(
                leaderNodeId = status.selfNodeId,
                ownerNodeId = ownerNodeId,
                term = status.term,
                seq = clusterMembershipRepository.nextSequence(),
                timestampMs = System.currentTimeMillis()
            )
            sendControlEnvelopeToHostOnly(busy, requesterHostAddress)
        }
    }

    private fun grantNextQueuedIfAny() {
        val status = clusterMembershipRepository.status.value
        if (status.role != pro.devapp.walkietalkiek.serivce.network.data.ClusterRole.LEADER) return
        while (true) {
            val nextTarget = floorArbitrationState.pollNextTarget() ?: return
            Timber.Forest.i("Granting queued floor to ${nextTarget.nodeId} host=${nextTarget.hostAddress}")
            val ownerToken = "node:${nextTarget.nodeId}"
            pttFloorRepository.acquire(ownerToken)
            val grant = ServerlessControlProtocol.floorGrantPacket(
                leaderNodeId = status.selfNodeId,
                targetNodeId = nextTarget.nodeId,
                term = status.term,
                seq = clusterMembershipRepository.nextSequence(),
                timestampMs = System.currentTimeMillis()
            )
            sendControlEnvelope(grant, nextTarget.hostAddress)
            return
        }
    }

    private fun sendControlEnvelope(data: ByteArray, preferredHostAddress: String?) {
        sendMessage(data)
        clientSocket.sendMessage(data)
        if (!preferredHostAddress.isNullOrBlank()) {
            sendMessageToHost(preferredHostAddress, data)
            clientSocket.sendMessageToHost(preferredHostAddress, data)
            acceptConnectionScope.launch {
                delay(120L)
                sendMessageToHost(preferredHostAddress, data)
                clientSocket.sendMessageToHost(preferredHostAddress, data)
            }
        }
    }

    private fun sendControlEnvelopeToHostOnly(data: ByteArray, preferredHostAddress: String) {
        sendMessageToHost(preferredHostAddress, data)
        clientSocket.sendMessageToHost(preferredHostAddress, data)
        acceptConnectionScope.launch {
            delay(120L)
            sendMessageToHost(preferredHostAddress, data)
            clientSocket.sendMessageToHost(preferredHostAddress, data)
        }
    }

    private fun sendMessageToHost(hostAddress: String, data: ByteArray) {
        outputQueueMap[hostAddress]?.offerLast(data.copyOf())
    }

    fun onLocalLeaderFloorReleased() {
        grantNextQueuedIfAny()
    }

    fun stop() {
        socket?.apply {
            close()
        }
        floorOwnerMonitorJob?.cancel()
        floorOwnerMonitorJob = null
        outputQueueMap.clear()
        floorArbitrationState.clear()
        readDataScope.cancel()
        writeDataScope.cancel()
        acceptConnectionScope.cancel()
    }

    fun sendMessage(data: ByteArray) {
        outputQueueMap.forEach { item ->
            item.value.offerLast(data.copyOf())
        }
    }
}

private const val AUDIO_PACKET_PREFIX: Byte = 1
private const val MAX_PACKET_SIZE = 256 * 1024
private const val DEFAULT_SERVER_PORT = 9915
private const val PORT_FALLBACK_SPAN = 5
