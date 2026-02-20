package pro.devapp.walkietalkiek.serivce.network.data

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresPermission
import pro.devapp.walkietalkiek.serivce.network.data.model.DeviceInfoModel
import pro.devapp.walkietalkiek.serivce.network.getDeviceID
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.ByteOrder

class DeviceInfoRepository(private val context: Context) {
    fun getCurrentDeviceInfo(): DeviceInfoModel {
        val defaultName =
            Build.MANUFACTURER + " " + Build.MODEL + " " + Build.VERSION.RELEASE + " " + Build.VERSION.SDK_INT
        return DeviceInfoModel(
            getDeviceID(context.contentResolver),
            defaultName,
            10
        )
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun getCurrentIp(): String? {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.activeNetwork?.let { network ->
            val linkProperties = connectivityManager.getLinkProperties(network)
            val ipv4FromActiveNetwork = linkProperties
                ?.linkAddresses
                ?.mapNotNull { it.address?.hostAddress }
                ?.firstOrNull { address ->
                    address.contains(':').not() && address.startsWith("127.").not()
                }
            if (!ipv4FromActiveNetwork.isNullOrBlank()) {
                return ipv4FromActiveNetwork
            }
        }

        val wifiManager = (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)
        var ipAddress = wifiManager.connectionInfo?.ipAddress ?: 0
        if (ipAddress == 0) {
            return null
        }

        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress)
        }
        val ipByteArray = ByteArray(4)
        ipByteArray[0] = (ipAddress shr 24 and 0xFF).toByte()
        ipByteArray[1] = (ipAddress shr 16 and 0xFF).toByte()
        ipByteArray[2] = (ipAddress shr 8 and 0xFF).toByte()
        ipByteArray[3] = (ipAddress and 0xFF).toByte()

        return try {
            InetAddress.getByAddress(ipByteArray).hostAddress
        } catch (_: UnknownHostException) {
            null
        }
    }
}
