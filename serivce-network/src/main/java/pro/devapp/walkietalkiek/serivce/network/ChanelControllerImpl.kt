package pro.devapp.walkietalkiek.serivce.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.SystemClock
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
import java.util.concurrent.ConcurrentHashMap

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
    private var pingJob: Job? = null
    private var localNodeId: String = ""
    private var localStartedAtMs: Long = 0L
    private var localStartedElapsedMs: Long = 0L
    private var lastSnapshotLoggedAtMs: Long = 0L
    private val resolvedEndpointAtMsByHost = ConcurrentHashMap<String, Long>()
    private var currentServerPort: Int = -1
    @Volatile
    private var nsdDiscovering: Boolean = false
    @Volatile
    private var nsdRegistered: Boolean = false
    private var nsdRetryAttempt: Int = 0

    override fun startDiscovery() {
        stopDiscovery()
        connectedDevicesRepository.clearAll()
        clusterMembershipRepository.clear()
        floorArbitrationState.clear()
        pttFloorRepository.clear()
        resolvedEndpointAtMsByHost.clear()
        localNodeId = deviceInfoRepository.getCurrentDeviceInfo().deviceId
        localStartedAtMs = System.currentTimeMillis()
        localStartedElapsedMs = SystemClock.elapsedRealtime()
        lastSnapshotLoggedAtMs = 0L
        nsdRetryAttempt = 0
        clusterMembershipRepository.initializeSelf(localNodeId, localStartedAtMs)
        pingJob?.cancel()
        pingScope?.cancel()
        pingScope = coroutineContextProvider.createScope(
            coroutineContextProvider.io
        )
        val port = server.initServer()
        currentServerPort = port
        registerNsdService(port)
        startHeartbeatLoop()
    }

    override fun stopDiscovery() {
        runCatching {
            if (nsdDiscovering) {
                nsdManager.stopServiceDiscovery(discoveryListener)
            }
        }.onFailure { error ->
            Timber.Forest.w(error, "stopServiceDiscovery failed")
        }
        runCatching {
            if (nsdRegistered) {
                nsdManager.unregisterService(registrationListener)
            }
        }.onFailure { error ->
            Timber.Forest.w(error, "unregisterService failed")
        }
        nsdDiscovering = false
        nsdRegistered = false
        client.stop()
        server.stop()
        currentServerPort = -1
        connectedDevicesRepository.clearAll()
        clusterMembershipRepository.clear()
        floorArbitrationState.clear()
        pttFloorRepository.clear()
        pingJob?.cancel()
        pingScope?.cancel()
        pingJob = null
        pingScope = null
    }

    fun onServiceRegister(serviceInfo: NsdServiceInfo) {
        currentServiceName = serviceInfo.serviceName
        nsdRegistered = true
        nsdRetryAttempt = 0
        Timber.Forest.i("onServiceRegister: using registered serviceName=%s", currentServiceName)
        runCatching {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
            nsdDiscovering = true
        }.onFailure { error ->
            nsdDiscovering = false
            Timber.Forest.w(error, "discoverServices failed after registration")
            scheduleNsdRestart("discoverServices exception after registration")
        }
    }

    fun onRegistrationFailed(errorCode: Int) {
        nsdRegistered = false
        Timber.Forest.w("NSD registration failed code=%d; scheduling restart", errorCode)
        scheduleNsdRestart("registration-failed:$errorCode")
    }

    fun onUnregistrationFailed(errorCode: Int) {
        Timber.Forest.w("NSD unregistration failed code=%d", errorCode)
    }

    fun onDiscoveryStarted() {
        nsdDiscovering = true
        nsdRetryAttempt = 0
    }

    fun onDiscoveryStartFailed(errorCode: Int) {
        nsdDiscovering = false
        Timber.Forest.w("NSD discovery start failed code=%d; scheduling restart", errorCode)
        scheduleNsdRestart("discovery-start-failed:$errorCode")
    }

    fun onDiscoveryStopped() {
        nsdDiscovering = false
    }

    fun onDiscoveryStopFailed(errorCode: Int) {
        nsdDiscovering = false
        Timber.Forest.w("NSD discovery stop failed code=%d", errorCode)
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
        }
    }

    private fun startHeartbeatLoop() {
        pingJob?.cancel()
        pingJob = pingScope?.launch {
            while (isActive) {
                connectedDevicesRepository.markStaleConnectionsDisconnected(STALE_CONNECTION_TIMEOUT_MS)
                clusterMembershipRepository.sweepStale(STALE_CONNECTION_TIMEOUT_MS, System.currentTimeMillis())
                ping()
                logSnapshotIfNeeded()
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    private fun scheduleNsdRestart(reason: String) {
        val scope = pingScope ?: return
        if (currentServerPort <= 0) {
            Timber.Forest.w("skip NSD restart: invalid server port reason=%s", reason)
            return
        }
        nsdRetryAttempt += 1
        val delayMs = (800L * nsdRetryAttempt.coerceAtMost(5)).coerceAtMost(4_000L)
        scope.launch {
            Timber.Forest.w("Scheduling NSD restart in %dms reason=%s attempt=%d", delayMs, reason, nsdRetryAttempt)
            delay(delayMs)
            if (!isActive) return@launch
            runCatching {
                if (nsdDiscovering) {
                    nsdManager.stopServiceDiscovery(discoveryListener)
                }
            }.onFailure { error ->
                Timber.Forest.w(error, "NSD restart: stopServiceDiscovery failed")
            }
            runCatching {
                if (nsdRegistered) {
                    nsdManager.unregisterService(registrationListener)
                }
            }.onFailure { error ->
                Timber.Forest.w(error, "NSD restart: unregisterService failed")
            }
            nsdDiscovering = false
            nsdRegistered = false
            registerNsdService(currentServerPort)
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

    private fun logSnapshotIfNeeded() {
        val now = System.currentTimeMillis()
        if (now - lastSnapshotLoggedAtMs < SNAPSHOT_LOG_INTERVAL_MS) {
            return
        }
        lastSnapshotLoggedAtMs = now
        val status = clusterMembershipRepository.status.value
        Timber.Forest.i(
            "%s snapshot self=%s role=%s leader=%s members=%d connected=%s",
            DIAG_PREFIX,
            resolveLocalNodeId(status.selfNodeId),
            status.role,
            status.leaderNodeId,
            status.activeMembersCount,
            connectedDevicesRepository.debugSummary(now)
        )
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
        Timber.Forest.i(
            "%s onServiceFound service=%s current=%s localNode=%s",
            DIAG_PREFIX,
            serviceInfo.serviceName,
            currentServiceName,
            resolveLocalNodeId()
        )
        if (isSelfService(serviceInfo.serviceName)) {
            Timber.Forest.i("onServiceFound: SELF")
            return
        }
        if (currentServiceName.isNullOrEmpty()) {
            Timber.Forest.i("onServiceFound: NAME NOT SET")
            return
        }

        clientInfoResolver.resolve(serviceInfo) { inetSocketAddress, nsdServiceInfo ->
            Timber.Forest.i(
                "%s resolved service=%s host=%s port=%d",
                DIAG_PREFIX,
                nsdServiceInfo.serviceName,
                inetSocketAddress.address?.hostAddress,
                inetSocketAddress.port
            )
            val hostAddress = inetSocketAddress.address?.hostAddress
            if (hostAddress.isNullOrBlank()) {
                Timber.Forest.w("Resolved host address is null/blank for ${nsdServiceInfo.serviceName}")
                return@resolve
            }
            val remoteNodeId = extractNodeIdFromServiceName(nsdServiceInfo.serviceName)
            if (remoteNodeId.isNotBlank() && remoteNodeId == resolveLocalNodeId()) {
                Timber.Forest.i("Ignore resolved self service by nodeId: %s", nsdServiceInfo.serviceName)
                return@resolve
            }
            val localIp = runCatching { deviceInfoRepository.getCurrentIp() }.getOrNull()
            if (!localIp.isNullOrBlank() && localIp == hostAddress) {
                Timber.Forest.i("Ignore resolved self service by host=%s", hostAddress)
                return@resolve
            }
            val shouldDial = shouldInitiateOutboundConnection(remoteNodeId)
            client.setOutboundDialPolicy(hostAddress, shouldDial)
            connectedDevicesRepository.addHostInfo(
                hostAddress,
                nsdServiceInfo.serviceName
            )
            if (shouldDial) {
                if (shouldSkipDuplicateResolve(hostAddress, inetSocketAddress.port)) {
                    Timber.Forest.i(
                        "%s skipResolveConnect host=%s port=%d reason=duplicateResolve",
                        DIAG_PREFIX,
                        hostAddress,
                        inetSocketAddress.port
                    )
                    return@resolve
                }
                client.addClient(inetSocketAddress)
            } else {
                Timber.Forest.i(
                    "Inbound-only mode for host=%s remoteNode=%s localNode=%s",
                    hostAddress,
                    remoteNodeId,
                    resolveLocalNodeId()
                )
            }
        }
    }

    override fun onServiceLost(nsdServiceInfo: NsdServiceInfo) {
        Timber.Forest.i("%s onServiceLost: %s", DIAG_PREFIX, nsdServiceInfo)
        if (isSelfService(nsdServiceInfo.serviceName)) {
            Timber.Forest.i("onServiceLost: SELF")
            return
        }
        // NSD can emit transient/false "lost" events during re-register/rejoin.
        // Do not hard-disconnect from discovery callback; rely on heartbeat/socket stale detection.
        val hostAddress = extractHostAddress(nsdServiceInfo)
        Timber.Forest.i(
            "%s onServiceLost ignored for stability; host=%s service=%s",
            DIAG_PREFIX,
            hostAddress,
            nsdServiceInfo.serviceName
        )
    }

    @Suppress("DEPRECATION")
    private fun extractHostAddress(nsdServiceInfo: NsdServiceInfo): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            nsdServiceInfo.hostAddresses.firstOrNull()?.hostAddress
        } else {
            nsdServiceInfo.host?.hostAddress
        }
    }

    private fun extractNodeIdFromServiceName(serviceName: String?): String {
        if (serviceName.isNullOrBlank()) return ""
        val parts = serviceName.split(":")
        return parts.getOrNull(1).orEmpty().trim()
    }

    private fun shouldInitiateOutboundConnection(remoteNodeId: String): Boolean {
        if (remoteNodeId.isBlank()) return true
        val local = resolveLocalNodeId()
        if (local.isBlank()) return true
        return local < remoteNodeId
    }

    private fun isSelfService(serviceName: String?): Boolean {
        if (serviceName.isNullOrBlank()) return false
        if (serviceName == currentServiceName) return true
        val nodeId = extractNodeIdFromServiceName(serviceName)
        return nodeId.isNotBlank() && nodeId == resolveLocalNodeId()
    }

    private fun shouldSkipDuplicateResolve(hostAddress: String, port: Int): Boolean {
        val now = SystemClock.elapsedRealtime()
        val key = "$hostAddress:$port"
        val last = resolvedEndpointAtMsByHost[key]
        return if (last != null && now - last < RESOLVE_DEDUP_WINDOW_MS) {
            true
        } else {
            resolvedEndpointAtMsByHost[key] = now
            false
        }
    }
}

private const val DIAG_PREFIX = "[DIAG_CTRL]"
private const val SNAPSHOT_LOG_INTERVAL_MS = 2_000L
private const val RESOLVE_DEDUP_WINDOW_MS = 3_000L
