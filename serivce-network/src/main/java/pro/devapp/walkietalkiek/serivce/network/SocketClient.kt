package pro.devapp.walkietalkiek.serivce.network

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pro.devapp.walkietalkiek.core.mvi.CoroutineContextProvider
import pro.devapp.walkietalkiek.serivce.network.data.ClusterMembershipRepository
import pro.devapp.walkietalkiek.serivce.network.data.ConnectedDevicesRepository
import pro.devapp.walkietalkiek.serivce.network.data.PttFloorRepository
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
    private val clusterMembershipRepository: ClusterMembershipRepository,
    private val pttFloorRepository: PttFloorRepository,
    private val coroutineContextProvider: CoroutineContextProvider
) {
    private val sockets = ConcurrentHashMap<String, Socket>()

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

    /**
     * Data for sending
     */
    private val outputQueueMap = ConcurrentHashMap<String, LinkedBlockingDeque<ByteArray>>()
    private val nodeHostMap = ConcurrentHashMap<String, String>()
    private val pendingFloorQueue = ArrayDeque<String>()

    fun sendMessage(data: ByteArray) {
        outputQueueMap.forEach { item ->
            item.value.offerLast(data.copyOf())
        }
    }

    fun sendMessageToHost(hostAddress: String, data: ByteArray) {
        outputQueueMap[hostAddress]?.offerLast(data.copyOf())
    }

    fun addClient(socketAddress: InetSocketAddress) {
        socketAddress.address.hostAddress?.let { hostAddress ->
            addClientScope.launch {
                lock.withLock {
                    try {
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
                        Timber.Forest.i("AddClient $hostAddress ${socketAddress.port}")
                        connectedDevicesRepository.addOrUpdateHostStateToConnected(hostAddress, socketAddress.port)
                        handleConnection(socket)
                    } catch (e: Exception) {
                        Timber.Forest.w(e)
                        Timber.Forest.i("connection error ${hostAddress} ${socketAddress.port}")
                        connectedDevicesRepository.setHostDisconnected(hostAddress)
                        // try reconnect
                        reconnectTimerScope.launch {
                            Timber.Forest.i("reconnect $hostAddress")
                            delay(1000L)
                            addClient(socketAddress)
                        }
                    }
                }
            }
        }
    }

    fun removeClient(hostAddress: String?) {
        hostAddress ?: return
        Timber.Forest.i("removeClient $hostAddress")
        val removedNodeIds = nodeHostMap.entries
            .filter { it.value == hostAddress }
            .map { it.key }
        if (removedNodeIds.isNotEmpty()) {
            removedNodeIds.forEach { nodeId ->
                nodeHostMap.remove(nodeId)
                pendingFloorQueue.remove(nodeId)
            }
        }
        outputQueueMap.remove(hostAddress)
        sockets[hostAddress]?.apply {
            val socketAddress = InetSocketAddress(
                hostAddress,
                port
            )
            close()
            sockets.remove(hostAddress)
            Timber.Forest.i("removeClient $hostAddress $port")
            connectedDevicesRepository.setHostDisconnected(hostAddress)
            // try reconnect
            reconnectTimerScope.launch {
                delay(1000L)
                Timber.Forest.i("reconnect $hostAddress $port")
                addClient(socketAddress)
            }
        }
    }

    fun stop() {
        sockets.forEach {
            it.value.close()
        }
        outputQueueMap.clear()
        pendingFloorQueue.clear()
        nodeHostMap.clear()
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
                                        nowMs = System.currentTimeMillis()
                                    )
                                }
                                is ControlEnvelope.FloorRequest -> {
                                    clusterMembershipRepository.onHeartbeat(
                                        nodeId = clusterControl.nodeId,
                                        term = clusterControl.term,
                                        timestampMs = clusterControl.timestampMs,
                                        nowMs = System.currentTimeMillis()
                                    )
                                    nodeHostMap[clusterControl.nodeId] = hostAddress
                                    handleFloorRequestAsLeader(clusterControl, hostAddress)
                                }
                                is ControlEnvelope.FloorGrant -> {
                                    pttFloorRepository.acquire("node:${clusterControl.targetNodeId}")
                                }
                                is ControlEnvelope.FloorRelease -> {
                                    pendingFloorQueue.remove(clusterControl.nodeId)
                                    val released = pttFloorRepository.releaseIfOwner("node:${clusterControl.nodeId}")
                                    val status = clusterMembershipRepository.status.value
                                    if (released && status.role == pro.devapp.walkietalkiek.serivce.network.data.ClusterRole.LEADER) {
                                        grantNextQueuedIfAny()
                                    }
                                }
                                is ControlEnvelope.FloorBusy -> {
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
                            }
                        }
                    }
                    connectedDevicesRepository.storeDataReceivedTime(hostAddress)
                }
            } catch (e: Exception) {
                Timber.Forest.w(e)
                removeClient(hostAddress)
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
                removeClient(hostAddress)
                Timber.Forest.i("remove $hostAddress")
            }
        }
    }

    private fun handleFloorRequestAsLeader(request: ControlEnvelope.FloorRequest, requesterHostAddress: String) {
        val status = clusterMembershipRepository.status.value
        if (status.role != pro.devapp.walkietalkiek.serivce.network.data.ClusterRole.LEADER) return
        val currentOwner = pttFloorRepository.currentFloorOwnerHost.value
        val requestedOwner = "node:${request.nodeId}"
        val shouldGrant = currentOwner == null || currentOwner == requestedOwner
        if (shouldGrant) {
            pendingFloorQueue.remove(request.nodeId)
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
            if (!pendingFloorQueue.contains(request.nodeId)) {
                pendingFloorQueue.addLast(request.nodeId)
            }
            val ownerNodeId = currentOwner?.removePrefix("node:")?.ifBlank { status.selfNodeId } ?: status.selfNodeId
            val busy = ServerlessControlProtocol.floorBusyPacket(
                leaderNodeId = status.selfNodeId,
                ownerNodeId = ownerNodeId,
                term = status.term,
                seq = clusterMembershipRepository.nextSequence(),
                timestampMs = System.currentTimeMillis()
            )
            sendControlEnvelope(busy, requesterHostAddress)
        }
    }

    private fun grantNextQueuedIfAny() {
        val status = clusterMembershipRepository.status.value
        if (status.role != pro.devapp.walkietalkiek.serivce.network.data.ClusterRole.LEADER) return
        while (pendingFloorQueue.isNotEmpty()) {
            val nextNodeId = pendingFloorQueue.removeFirst()
            val nextHostAddress = nodeHostMap[nextNodeId]
            if (nextHostAddress.isNullOrBlank()) {
                continue
            }
            val ownerToken = "node:$nextNodeId"
            pttFloorRepository.acquire(ownerToken)
            val grant = ServerlessControlProtocol.floorGrantPacket(
                leaderNodeId = status.selfNodeId,
                targetNodeId = nextNodeId,
                term = status.term,
                seq = clusterMembershipRepository.nextSequence(),
                timestampMs = System.currentTimeMillis()
            )
            sendControlEnvelope(grant, nextHostAddress)
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

    fun onLocalLeaderFloorReleased() {
        grantNextQueuedIfAny()
    }
}

private const val AUDIO_PACKET_PREFIX: Byte = 1
private const val MAX_PACKET_SIZE = 256 * 1024
