package pro.devapp.walkietalkiek.serivce.network

import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pro.devapp.walkietalkiek.core.mvi.CoroutineContextProvider
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
import kotlin.random.Random

class SocketServer(
    private val connectedDevicesRepository: ConnectedDevicesRepository,
    private val clientSocket: SocketClient,
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

    private val SERVER_PORT by lazy {
        getPort().also { port ->
            Timber.Forest.i("Server port initialized: $port")
        }
    }

    private fun getPort(): Int {
     //   return 9915
        return Random.nextInt(8111, 9999).also { port ->
            Timber.Forest.i("Generated random port: $port")
        }
    }

    /**
     * Data for sending
     */
    private val outputQueueMap = ConcurrentHashMap<String, LinkedBlockingDeque<ByteArray>>()

    private var socket: ServerSocket? = null

    var dataListener: ((bytes: ByteArray) -> Unit)? = null

    fun initServer(): Int {
        if (socket != null && socket?.isClosed == false) {
            return SERVER_PORT
        }
        socket = ServerSocket(SERVER_PORT).apply {
            reuseAddress = false
            soTimeout = 5000 // Set a timeout for accept to avoid blocking indefinitely
        }
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
        return SERVER_PORT
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
        connectedDevicesRepository.storeDataReceivedTime(hostAddress)
    }

    fun stop() {
        socket?.apply {
            close()
        }
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
