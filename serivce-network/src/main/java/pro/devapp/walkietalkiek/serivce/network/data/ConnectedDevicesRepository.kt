package pro.devapp.walkietalkiek.serivce.network.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pro.devapp.walkietalkiek.serivce.network.data.model.ClientModel
import java.util.Date

/**
 * Store information about connected devices
 */
class ConnectedDevicesRepository {
    private val lock = Any()
    private val clients = HashMap<String, ClientModel>()

    private val _clientsFlow = MutableStateFlow<List<ClientModel>>(emptyList())
    val clientsFlow: StateFlow<List<ClientModel>>
        get() = _clientsFlow

    private fun publishChangesLocked() {
        val clientsList = clients.values
            .sortedWith(compareByDescending<ClientModel> { it.isConnected }.thenBy { it.hostAddress })
        if (clientsList != _clientsFlow.value) {
            _clientsFlow.value = clientsList
        }
    }

    fun getClientByAddress(hostAddress: String): ClientModel? {
        synchronized(lock) {
            return clients[hostAddress]
        }
    }

    fun addOrUpdateHostStateToConnected(hostAddress: String, port: Int) {
        synchronized(lock) {
            val now = Date().time
            val current = clients[hostAddress]
            val updated = ClientModel(
                hostAddress = hostAddress,
                isConnected = true,
                hostName = current?.hostName.orEmpty(),
                port = port,
                lastDataReceivedAt = now
            )
            clients[hostAddress] = updated
            if (updated != current) {
                publishChangesLocked()
            }
        }
    }

    fun setHostDisconnected(hostAddress: String) {
        synchronized(lock) {
            val current = clients[hostAddress] ?: return
            val updated = current.copy(isConnected = false)
            clients[hostAddress] = updated
            if (updated != current) {
                publishChangesLocked()
            }
        }
    }

    fun setHostDisconnectedByName(hostName: String) {
        synchronized(lock) {
            var changed = false
            clients.forEach { (hostAddress, client) ->
                if (client.hostName == hostName && client.isConnected) {
                    clients[hostAddress] = client.copy(isConnected = false)
                    changed = true
                }
            }
            if (changed) {
                publishChangesLocked()
            }
        }
    }

    fun addHostInfo(hostAddress: String, name: String) {
        synchronized(lock) {
            val current = clients[hostAddress]
            val updated = ClientModel(
                hostAddress = hostAddress,
                hostName = name,
                isConnected = current?.isConnected == true,
                port = current?.port ?: 0,
                lastDataReceivedAt = current?.lastDataReceivedAt ?: 0,
            )
            clients[hostAddress] = updated
            if (updated != current) {
                publishChangesLocked()
            }
        }
    }

    fun storeDataReceivedTime(hostAddress: String) {
        synchronized(lock) {
            val now = Date().time
            val current = clients[hostAddress]
            val updated = ClientModel(
                hostAddress = hostAddress,
                hostName = current?.hostName.orEmpty(),
                // Any incoming data means the peer is alive right now.
                isConnected = true,
                port = current?.port ?: 0,
                lastDataReceivedAt = now,
            )
            clients[hostAddress] = updated
            // Avoid flooding the UI with state emissions for every packet; emit only on state changes.
            if (current == null || current.isConnected != updated.isConnected || current.port != updated.port || current.hostName != updated.hostName) {
                publishChangesLocked()
            }
        }
    }

    fun markStaleConnectionsDisconnected(maxSilenceMs: Long, nowMillis: Long = Date().time) {
        synchronized(lock) {
            var changed = false
            clients.forEach { (hostAddress, client) ->
                val isStale = client.isConnected &&
                    client.lastDataReceivedAt > 0L &&
                    nowMillis - client.lastDataReceivedAt > maxSilenceMs
                if (isStale) {
                    clients[hostAddress] = client.copy(isConnected = false)
                    changed = true
                }
            }
            if (changed) {
                publishChangesLocked()
            }
        }
    }

    fun clearAll() {
        synchronized(lock) {
            clients.clear()
            publishChangesLocked()
        }
    }
}
