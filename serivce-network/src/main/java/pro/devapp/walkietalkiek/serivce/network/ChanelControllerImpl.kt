package pro.devapp.walkietalkiek.serivce.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pro.devapp.walkietalkiek.core.mvi.CoroutineContextProvider
import pro.devapp.walkietalkiek.serivce.network.data.ConnectedDevicesRepository
import pro.devapp.walkietalkiek.serivce.network.data.DeviceInfoRepository
import pro.devapp.walkietalkiek.serivce.network.data.PttFloorRepository
import timber.log.Timber

private const val SERVICE_TYPE = "_wfwt._tcp" /* WiFi Walkie Talkie */
private const val STALE_CONNECTION_TIMEOUT_MS = 15_000L

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
    private val pttFloorRepository: PttFloorRepository,
    private val client: SocketClient,
    private val server: SocketServer,
    private val coroutineContextProvider: CoroutineContextProvider,
    private val clientInfoResolver: ClientInfoResolver
): MessageController, ClientController {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val discoveryListener = DiscoveryListener(this)
    private val registrationListener = RegistrationListener(this)

    private var currentServiceName: String? = null

    private var pingScope: CoroutineScope? = null

    override fun startDiscovery() {
        connectedDevicesRepository.clearAll()
        pttFloorRepository.clear()
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
                    ping()
                    delay(5000L)
                }
            }
        }
    }

    private fun ping() {
        server.sendMessage("ping".toByteArray())
        client.sendMessage("ping".toByteArray())
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
