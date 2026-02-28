package pro.devapp.walkietalkiek.serivce.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.SystemClock
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pro.devapp.walkietalkiek.core.mvi.CoroutineContextProvider
import pro.devapp.walkietalkiek.core.flags.FeatureFlagsRepository
import pro.devapp.walkietalkiek.serivce.network.data.ConnectedDevicesRepository
import pro.devapp.walkietalkiek.serivce.network.data.ClusterMembershipRepository
import pro.devapp.walkietalkiek.serivce.network.data.DeviceInfoRepository
import pro.devapp.walkietalkiek.serivce.network.data.PttFloorRepository
import timber.log.Timber

private const val SERVICE_TYPE = "_wfwt._tcp" /* WiFi Walkie Talkie */
private const val STALE_CONNECTION_TIMEOUT_MS = 5_000L
private const val HEARTBEAT_INTERVAL_MS = 1_000L

interface MessageController{
    fun sendMessage(data: ByteArray)
}

interface ClientController{
    fun startDiscovery()
    fun stopDiscovery()
    fun onServiceFound(serviceInfo: NsdServiceInfo)
    fun onServiceLost(nsdServiceInfo: NsdServiceInfo)
}

internal class ChanelControllerImpl(
    context: Context,
    private val deviceInfoRepository: DeviceInfoRepository,
    private val connectedDevicesRepository: ConnectedDevicesRepository,
    private val clusterMembershipRepository: ClusterMembershipRepository,
    private val floorArbitrationState: FloorArbitrationState,
    private val pttFloorRepository: PttFloorRepository,
    private val featureFlagsRepository: FeatureFlagsRepository,
    private val client: SocketClient,
    private val server: SocketServer,
    private val coroutineContextProvider: CoroutineContextProvider,
    private val clientInfoResolver: ClientInfoResolver
): MessageController, ClientController, FloorLeaseController {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val discoveryListener = DiscoveryListener(this)
    private val registrationListener = RegistrationListener(this)

    private var currentServiceName: String? = null

    private var pingScope: CoroutineScope? = null
    private var localNodeId: String = ""
    private var localStartedAtMs: Long = 0L
    private var localStartedElapsedMs: Long = 0L

    override fun startDiscovery() {
        connectedDevicesRepository.clearAll()
        clusterMembershipRepository.clear()
        floorArbitrationState.clear()
        pttFloorRepository.clear()
        localNodeId = deviceInfoRepository.getCurrentDeviceInfo().deviceId
        localStartedAtMs = System.currentTimeMillis()
        localStartedElapsedMs = SystemClock.elapsedRealtime()
        clusterMembershipRepository.initializeSelf(localNodeId, localStartedAtMs)
        pingScope = coroutineContextProvider.createScope(
            coroutineContextProvider.io
        )
        val port = server.initServer()
        registerNsdService(port)
    }

    override fun stopDiscovery() {
        nsdManager.apply {
            stopServiceDiscovery(discoveryListener)
            unregisterService(registrationListener)
        }
        client.stop()
        server.stop()
        connectedDevicesRepository.clearAll()
        clusterMembershipRepository.clear()
        floorArbitrationState.clear()
        pttFloorRepository.clear()
        pingScope?.cancel()
    }

    fun onServiceRegister() {
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    override fun sendMessage(data: ByteArray) {
        client.sendMessage(data)
        server.sendMessage(data)
    }

    override fun requestFloor(): FloorLeaseRequestResult {
        val status = clusterMembershipRepository.status.value
        if (!featureFlagsRepository.flags.value.serverlessControl) {
            sendMessage(FloorControlProtocol.acquirePacket())
            return FloorLeaseRequestResult.Granted
        }
        val localNodeIdResolved = resolveLocalNodeId(status.selfNodeId)
        if (localNodeIdResolved.isBlank()) {
            Timber.Forest.w("requestFloor: local node id is blank; returning pending")
            return FloorLeaseRequestResult.Pending
        }
        val now = System.currentTimeMillis()
        val term = status.term
        val seq = clusterMembershipRepository.nextSequence()
        val selfToken = "node:$localNodeIdResolved"
        val owner = pttFloorRepository.currentFloorOwnerHost.value
        if (owner.equals(selfToken, ignoreCase = true)) {
            Timber.Forest.i("requestFloor: already floor owner ($selfToken). Granting locally.")
            return FloorLeaseRequestResult.Granted
        }
        if (status.role == pro.devapp.walkietalkiek.serivce.network.data.ClusterRole.LEADER) {
            val canGrantToSelf = owner == null || owner == selfToken
            if (canGrantToSelf) {
                pttFloorRepository.acquire(selfToken)
                val grant = ServerlessControlProtocol.floorGrantPacket(
                    leaderNodeId = localNodeIdResolved,
                    targetNodeId = localNodeIdResolved,
                    term = term,
                    seq = seq,
                    timestampMs = now
                )
                sendMessage(grant)
                return FloorLeaseRequestResult.Granted
            }
            return FloorLeaseRequestResult.Pending
        }
        val request = ServerlessControlProtocol.floorRequestPacket(
            nodeId = localNodeIdResolved,
            term = term,
            seq = seq,
            timestampMs = now
        )
        sendMessage(request)
        return FloorLeaseRequestResult.Pending
    }

    override fun releaseFloor() {
        val status = clusterMembershipRepository.status.value
        if (!featureFlagsRepository.flags.value.serverlessControl) {
            sendMessage(FloorControlProtocol.releasePacket())
            return
        }
        val now = System.currentTimeMillis()
        val term = status.term
        val seq = clusterMembershipRepository.nextSequence()
        val release = ServerlessControlProtocol.floorReleasePacket(
            nodeId = resolveLocalNodeId(status.selfNodeId),
            term = term,
            seq = seq,
            timestampMs = now
        )
        if (status.role == pro.devapp.walkietalkiek.serivce.network.data.ClusterRole.LEADER) {
            val released = pttFloorRepository.releaseIfOwner(localFloorToken())
            if (released) {
                server.onLocalLeaderFloorReleased()
                client.onLocalLeaderFloorReleased()
            }
        }
        sendMessage(release)
    }

    private fun registerNsdService(port: Int) {
        Timber.Forest.i("registerService")
        val result = deviceInfoRepository.getCurrentDeviceInfo()
        result.apply {
            // Android NSD implementation is very unstable when services
            // registers with the same name. Will use "CHANNEL_NAME:DEVICE_ID:".
            val serviceInfo = NsdServiceInfo()
            val encodedName = Base64.encodeToString(
                name.toByteArray(),
                Base64.NO_PADDING or Base64.NO_WRAP
            )
            val serviceName = "$encodedName:$deviceId:"
            serviceInfo.serviceType = SERVICE_TYPE
            serviceInfo.serviceName = serviceName
            currentServiceName = serviceName
            serviceInfo.port = port
            Timber.Forest.i("try register $name: $serviceInfo")
            nsdManager.registerService(
                serviceInfo,
                NsdManager.PROTOCOL_DNS_SD,
                registrationListener
            )
            pingScope?.launch {
                while (isActive) {
                    connectedDevicesRepository.markStaleConnectionsDisconnected(STALE_CONNECTION_TIMEOUT_MS)
                    clusterMembershipRepository.sweepStale(STALE_CONNECTION_TIMEOUT_MS, System.currentTimeMillis())
                    ping()
                    delay(HEARTBEAT_INTERVAL_MS)
                }
            }
        }
    }

    private fun ping() {
        if (localNodeId.isNotBlank()) {
            val now = System.currentTimeMillis()
            clusterMembershipRepository.onHeartbeat(
                nodeId = localNodeId,
                term = 0L,
                timestampMs = now,
                nowMs = now,
                joinedAtMs = localStartedAtMs,
                uptimeMs = (SystemClock.elapsedRealtime() - localStartedElapsedMs).coerceAtLeast(0L)
            )
            val heartbeat = ServerlessControlProtocol.heartbeatPacket(
                nodeId = localNodeId,
                term = 0L,
                seq = clusterMembershipRepository.nextSequence(),
                timestampMs = now,
                startedAtMs = localStartedAtMs,
                uptimeMs = (SystemClock.elapsedRealtime() - localStartedElapsedMs).coerceAtLeast(0L)
            )
            server.sendMessage(heartbeat)
            client.sendMessage(heartbeat)
        }
        server.sendMessage("ping".toByteArray())
        client.sendMessage("ping".toByteArray())
    }

    private fun localFloorToken(): String = "node:${resolveLocalNodeId()}"

    private fun resolveLocalNodeId(statusSelfNodeId: String = ""): String {
        val resolved = localNodeId.ifBlank { statusSelfNodeId }.ifBlank {
            deviceInfoRepository.getCurrentDeviceInfo().deviceId
        }
        if (localNodeId.isBlank() && resolved.isNotBlank()) {
            localNodeId = resolved
        }
        return resolved
    }

    override fun onServiceFound(serviceInfo: NsdServiceInfo) {
        // check for self add to list
        Timber.Forest.i("onServiceFound: ${serviceInfo.serviceName} current: $currentServiceName")
        if (serviceInfo.serviceName == currentServiceName) {
            Timber.Forest.i("onServiceFound: SELF")
            return
        }
        if (currentServiceName.isNullOrEmpty()) {
            Timber.Forest.i("onServiceFound: NAME NOT SET")
            return
        }

        clientInfoResolver.resolve(serviceInfo) { inetSocketAddress, nsdServiceInfo ->
            Timber.Forest.i("Resolve: ${nsdServiceInfo.serviceName}")
            val hostAddress = inetSocketAddress.address?.hostAddress
            if (hostAddress.isNullOrBlank()) {
                Timber.Forest.w("Resolved host address is null/blank for ${nsdServiceInfo.serviceName}")
                return@resolve
            }
            connectedDevicesRepository.addHostInfo(
                hostAddress,
                nsdServiceInfo.serviceName
            )
            client.addClient(inetSocketAddress)
        }
    }

    override fun onServiceLost(nsdServiceInfo: NsdServiceInfo) {
        Timber.Forest.i("onServiceLost: $nsdServiceInfo")
        val hostAddress = extractHostAddress(nsdServiceInfo)
        if (!hostAddress.isNullOrBlank()) {
            connectedDevicesRepository.setHostDisconnected(hostAddress)
        } else {
            connectedDevicesRepository.setHostDisconnectedByName(nsdServiceInfo.serviceName)
        }

        if (nsdServiceInfo.serviceName == currentServiceName) {
            Timber.Forest.i("onServiceLost: SELF")
            return
        }
    }

    @Suppress("DEPRECATION")
    private fun extractHostAddress(nsdServiceInfo: NsdServiceInfo): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            nsdServiceInfo.hostAddresses.firstOrNull()?.hostAddress
        } else {
            nsdServiceInfo.host?.hostAddress
        }
    }
}
