package pro.devapp.walkietalkiek.serivce.network

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pro.devapp.walkietalkiek.core.mvi.CoroutineContextProvider
import pro.devapp.walkietalkiek.serivce.network.data.ClusterMembershipRepository
import pro.devapp.walkietalkiek.serivce.network.data.ConnectedDevicesRepository
import pro.devapp.walkietalkiek.serivce.network.data.PttFloorRepository
import pro.devapp.walkietalkiek.serivce.network.data.TextMessagesRepository
import timber.log.Timber
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit

class SocketClient (
    private val connectedDevicesRepository: ConnectedDevicesRepository,
    private val floorArbitrationState: FloorArbitrationState,
    private val clusterMembershipRepository: ClusterMembershipRepository,
    private val pttFloorRepository: PttFloorRepository,
    private val textMessagesRepository: TextMessagesRepository,
    private val coroutineContextProvider: CoroutineContextProvider
) {
    private val sockets = ConcurrentHashMap<String, Socket>()
    private val dialPolicyByHost = ConcurrentHashMap<String, Boolean>()
    private val latestEndpointByHost = ConcurrentHashMap<String, InetSocketAddress>()
    private val reconnectJobsByHost = ConcurrentHashMap<String, Job>()

    var dataListener: ((bytes: ByteArray) -> Unit)? = null

    private val lock = Mutex()

    private val reconnectTimerScope = coroutineContextProvider.createScope(
        coroutineContextProvider.io
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val addClientScope = coroutineContextProvider.createScope(
        coroutineContextProvider.io.limitedParallelism(1)
    )

    private val clientsScope = coroutineContextProvider.createScope(
        coroutineContextProvider.io
    )

    private val readDataScope = coroutineContextProvider.createScope(
        coroutineContextProvider.io
    )

    private val writeDataScope = coroutineContextProvider.createScope(
        coroutineContextProvider.io
    )
    private var floorOwnerMonitorJob: Job? = null

    /**
     * Data for sending
     */
    private val outputQueueMap = ConcurrentHashMap<String, LinkedBlockingDeque<ByteArray>>()

    init {
        startFloorOwnerMonitorIfNeeded()
    }

    fun sendMessage(data: ByteArray) {
        outputQueueMap.forEach { item ->
            item.value.offerLast(data.copyOf())
        }
    }

    fun sendMessageToHost(hostAddress: String, data: ByteArray) {
        outputQueueMap[hostAddress]?.offerLast(data.copyOf())
    }

    fun setOutboundDialPolicy(hostAddress: String, shouldDial: Boolean) {
        if (hostAddress.isBlank()) return
        dialPolicyByHost[hostAddress] = shouldDial
        Timber.Forest.i("%s dialPolicy host=%s shouldDial=%s", DIAG_PREFIX, hostAddress, shouldDial)
        if (!shouldDial) {
            closeOutboundConnection(hostAddress, markDisconnected = false)
        }
    }

    private fun canDialHost(hostAddress: String): Boolean {
        return dialPolicyByHost[hostAddress] != false
    }

    fun addClient(socketAddress: InetSocketAddress) {
        socketAddress.address.hostAddress?.let { hostAddress ->
            latestEndpointByHost[hostAddress] = socketAddress
            reconnectJobsByHost.remove(hostAddress)?.cancel()
            if (!canDialHost(hostAddress)) {
                Timber.Forest.i("%s skipConnect host=%s reason=dialPolicyDisabled", DIAG_PREFIX, hostAddress)
                return
            }
            addClientScope.launch {
                lock.withLock {
                    try {
                        if (!canDialHost(hostAddress)) {
                            Timber.Forest.i("%s skipConnect host=%s reason=dialPolicyDisabledAfterSchedule", DIAG_PREFIX, hostAddress)
                            return@withLock
                        }
                        val existingSocket = sockets[hostAddress]
                        if (existingSocket != null &&
                            !existingSocket.isClosed &&
                            existingSocket.isConnected &&
                            existingSocket.port == socketAddress.port
                        ) {
                            Timber.Forest.i(
                                "%s skipConnect host=%s port=%d reason=alreadyConnected",
                                DIAG_PREFIX,
                                hostAddress,
                                socketAddress.port
                            )
                            return@withLock
                        }
                        sockets[hostAddress]?.apply {
                            close()
                            sockets.remove(hostAddress)
                        }
                        while (sockets[hostAddress]?.isClosed?.not() == true) {
                            Timber.Forest.i("Waiting for socket to close $hostAddress")
                            delay(100L)
                        }
                        val socket = Socket(hostAddress, socketAddress.port).apply { //TODO add port provider
                            tcpNoDelay = true
                        }
                        socket.receiveBufferSize = 8192 * 2
                        sockets[hostAddress] = socket
                        outputQueueMap[hostAddress] = LinkedBlockingDeque()
                        Timber.Forest.i("%s connected host=%s port=%d activeSockets=%d", DIAG_PREFIX, hostAddress, socketAddress.port, sockets.size)
                        connectedDevicesRepository.addOrUpdateHostStateToConnected(hostAddress, socketAddress.port)
                        handleConnection(socket)
                    } catch (e: Exception) {
                        Timber.Forest.w(e)
                        Timber.Forest.i("%s connectError host=%s port=%d", DIAG_PREFIX, hostAddress, socketAddress.port)
                        if (sockets[hostAddress]?.isConnected != true) {
                            connectedDevicesRepository.setHostDisconnected(hostAddress)
                        }
                        // try reconnect
                        scheduleReconnect(hostAddress)
                    }
                }
            }
        }
    }

    fun removeClient(hostAddress: String?) {
        removeClient(hostAddress, expectedSocket = null)
    }

    private fun removeClient(hostAddress: String?, expectedSocket: Socket?) {
        hostAddress ?: return
        Timber.Forest.i("%s removeClient host=%s", DIAG_PREFIX, hostAddress)
        if (expectedSocket != null) {
            val currentSocket = sockets[hostAddress]
            if (currentSocket != null && currentSocket !== expectedSocket) {
                Timber.Forest.i(
                    "%s skipRemove host=%s reason=staleSocketCallback",
                    DIAG_PREFIX,
                    hostAddress
                )
                return
            }
        }
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
        outputQueueMap.remove(hostAddress)
        sockets[hostAddress]?.apply {
            close()
            sockets.remove(hostAddress)
            Timber.Forest.i("%s socketClosed host=%s port=%d activeSockets=%d", DIAG_PREFIX, hostAddress, port, sockets.size)
            connectedDevicesRepository.setHostDisconnected(hostAddress)
            // try reconnect
            scheduleReconnect(hostAddress)
        }
    }

    fun stop() {
        sockets.forEach {
            it.value.close()
        }
        floorOwnerMonitorJob?.cancel()
        floorOwnerMonitorJob = null
        outputQueueMap.clear()
        dialPolicyByHost.clear()
        latestEndpointByHost.clear()
        reconnectJobsByHost.values.forEach { it.cancel() }
        reconnectJobsByHost.clear()
        floorArbitrationState.clear()
        reconnectTimerScope.cancel()
        addClientScope.cancel()
        clientsScope.cancel()
        readDataScope.cancel()
        writeDataScope.cancel()
    }

    private fun handleConnection(socket: Socket) {
        val hostAddress = socket.inetAddress?.hostAddress
        if (hostAddress.isNullOrBlank()) {
            Timber.Forest.w("Socket host address is null/blank. Closing socket.")
            socket.close()
            return
        }
        val readingFuture = readDataScope.launch {
            try {
                val dataInput = DataInputStream(socket.getInputStream())
                Timber.Forest.i("Started reading $hostAddress")
                while (!socket.isClosed && !socket.isInputShutdown) {
                    val packetSize = dataInput.readInt()
                    if (packetSize <= 0 || packetSize > MAX_PACKET_SIZE) {
                        Timber.Forest.w("Skip invalid packet size: $packetSize")
                        continue
                    }
                    val data = ByteArray(packetSize)
                    dataInput.readFully(data)
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
                            } else if (message == "ping") {
                                sendMessageToHost(
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
            } catch (e: Exception) {
                Timber.Forest.w(e)
                removeClient(hostAddress, expectedSocket = socket)
            } finally {

            }
        }
        writeDataScope.launch {
            try {
                val outputStream = DataOutputStream(socket.getOutputStream())
                var errorCounter = 0
                while (socket.isConnected && !socket.isClosed) {
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
                            throw e
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.Forest.w(e)
            } finally {
                readingFuture.cancel()
                removeClient(hostAddress, expectedSocket = socket)
                Timber.Forest.i("remove $hostAddress")
            }
        }
    }

    private fun closeOutboundConnection(hostAddress: String, markDisconnected: Boolean) {
        val existing = sockets.remove(hostAddress) ?: return
        outputQueueMap.remove(hostAddress)
        runCatching { existing.close() }
            .onFailure { Timber.Forest.w(it, "Failed closing outbound socket for $hostAddress") }
        if (markDisconnected) {
            connectedDevicesRepository.setHostDisconnected(hostAddress)
        }
        Timber.Forest.i("%s closeOutbound host=%s markDisconnected=%s", DIAG_PREFIX, hostAddress, markDisconnected)
    }

    private fun scheduleReconnect(hostAddress: String) {
        if (!canDialHost(hostAddress)) return
        val target = latestEndpointByHost[hostAddress] ?: return
        reconnectJobsByHost.remove(hostAddress)?.cancel()
        reconnectJobsByHost[hostAddress] = reconnectTimerScope.launch {
            delay(1000L)
            if (!canDialHost(hostAddress)) return@launch
            val latestTarget = latestEndpointByHost[hostAddress] ?: return@launch
            Timber.Forest.i(
                "%s reconnect host=%s port=%d",
                DIAG_PREFIX,
                hostAddress,
                latestTarget.port
            )
            addClient(latestTarget)
        }
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
        if (!preferredHostAddress.isNullOrBlank()) {
            sendMessageToHost(preferredHostAddress, data)
            clientsScope.launch {
                delay(120L)
                sendMessageToHost(preferredHostAddress, data)
            }
        }
    }

    private fun sendControlEnvelopeToHostOnly(data: ByteArray, preferredHostAddress: String) {
        sendMessageToHost(preferredHostAddress, data)
        clientsScope.launch {
            delay(120L)
            sendMessageToHost(preferredHostAddress, data)
        }
    }

    fun onLocalLeaderFloorReleased() {
        grantNextQueuedIfAny()
    }

    private fun startFloorOwnerMonitorIfNeeded() {
        if (floorOwnerMonitorJob?.isActive == true) return
        floorOwnerMonitorJob = clientsScope.launch {
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
}

private const val AUDIO_PACKET_PREFIX: Byte = 1
private const val MAX_PACKET_SIZE = 256 * 1024
private const val DIAG_PREFIX = "[DIAG_SOCK_CLIENT]"
