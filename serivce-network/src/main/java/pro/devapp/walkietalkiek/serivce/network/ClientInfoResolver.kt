package pro.devapp.walkietalkiek.serivce.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import kotlinx.coroutines.launch
import pro.devapp.walkietalkiek.core.mvi.CoroutineContextProvider
import timber.log.Timber
import java.net.InetSocketAddress
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

internal class ClientInfoResolver(
    private val context: Context,
    private val coroutineContextProvider: CoroutineContextProvider
) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val scope = coroutineContextProvider.createScope(
        coroutineContextProvider.io
    )

    private val executor = ThreadPoolExecutor(
        0, 1, 60L, TimeUnit.SECONDS,
        LinkedBlockingQueue<Runnable>()
    )

    fun resolve(
        nsdInfo: NsdServiceInfo,
        resultListener: (socketAddress: InetSocketAddress, nsdServiceInfo: NsdServiceInfo) -> Unit
        ) {
        Timber.i("resolve")
        scope.launch {
            Timber.i("resolveNext $nsdInfo")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                resolveWithServiceInfoCallback(nsdInfo, resultListener)
            } else {
                resolveWithLegacyApi(nsdInfo, resultListener)
            }
        }
    }

    private fun resolveWithServiceInfoCallback(
        nsdInfo: NsdServiceInfo,
        resultListener: (socketAddress: InetSocketAddress, nsdServiceInfo: NsdServiceInfo) -> Unit
    ) {
        nsdManager.registerServiceInfoCallback(
            nsdInfo,
            executor,
            object : NsdManager.ServiceInfoCallback {
                override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) {
                    Timber.i("onResolveFailed: $errorCode")
                }

                override fun onServiceUpdated(serviceInfo: NsdServiceInfo) {
                    val hostAddress = serviceInfo.hostAddresses.firstOrNull()
                    if (hostAddress == null) {
                        Timber.i("onServiceUpdated without host: $serviceInfo")
                        return
                    }
                    val socketAddress = InetSocketAddress(hostAddress, serviceInfo.port)
                    Timber.i("onServiceResolved: $socketAddress")
                    if (!socketAddress.address.isMulticastAddress) {
                        resultListener(socketAddress, serviceInfo)
                    }
                }

                override fun onServiceLost() {
                    Timber.i("onServiceLost")
                }

                override fun onServiceInfoCallbackUnregistered() {
                    Timber.i("onServiceInfoCallbackUnregistered")
                }
            }
        )
    }

    private fun resolveWithLegacyApi(
        nsdInfo: NsdServiceInfo,
        resultListener: (socketAddress: InetSocketAddress, nsdServiceInfo: NsdServiceInfo) -> Unit
    ) {
        nsdManager.resolveService(nsdInfo, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Timber.i("onResolveFailed: $errorCode $serviceInfo")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                val host = serviceInfo.host
                if (host == null) {
                    Timber.i("onServiceResolved without host: $serviceInfo")
                    return
                }
                val socketAddress = InetSocketAddress(host, serviceInfo.port)
                Timber.i("onServiceResolved: $socketAddress")
                if (!socketAddress.address.isMulticastAddress) {
                    resultListener(socketAddress, serviceInfo)
                }
            }
        })
    }
}
