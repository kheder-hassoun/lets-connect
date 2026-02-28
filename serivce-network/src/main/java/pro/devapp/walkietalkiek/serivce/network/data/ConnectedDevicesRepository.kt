package pro.devapp.walkietalkiek.serivce.network.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pro.devapp.walkietalkiek.serivce.network.data.model.ClientModel
import timber.log.Timber
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
                Timber.Forest.i(
                    "%s connected host=%s port=%d prevConnected=%s summary=%s",
                    DIAG_PREFIX,
                    hostAddress,
                    port,
                    current?.isConnected,
                    debugSummaryLocked(now)
                )
                publishChangesLocked()
            }
        }
    }

    fun setHostDisconnected(hostAddress: String, nowMillis: Long = Date().time) {
        synchronized(lock) {
            val current = clients[hostAddress] ?: return
            val shouldDisconnect = current.lastDataReceivedAt <= 0L ||
                nowMillis - current.lastDataReceivedAt > DISCONNECT_DEBOUNCE_MS
            if (!shouldDisconnect) {
                // Avoid flapping when one socket path closes but another path is still alive.
                Timber.Forest.i(
                    "%s disconnect skipped host=%s recentDataAgeMs=%d summary=%s",
                    DIAG_PREFIX,
                    hostAddress,
                    (nowMillis - current.lastDataReceivedAt).coerceAtLeast(0L),
                    debugSummaryLocked(nowMillis)
                )
                return
            }
            val updated = current.copy(isConnected = false)
            clients[hostAddress] = updated
            if (updated != current) {
                Timber.Forest.i(
                    "%s disconnected host=%s reason=explicit summary=%s",
                    DIAG_PREFIX,
                    hostAddress,
                    debugSummaryLocked(nowMillis)
                )
                publishChangesLocked()
            }
        }
    }

    fun setHostDisconnectedByName(hostName: String, nowMillis: Long = Date().time) {
        synchronized(lock) {
            var changed = false
            clients.forEach { (hostAddress, client) ->
                val shouldDisconnect = client.lastDataReceivedAt <= 0L ||
                    nowMillis - client.lastDataReceivedAt > DISCONNECT_DEBOUNCE_MS
                if (client.hostName == hostName && client.isConnected && shouldDisconnect) {
                    clients[hostAddress] = client.copy(isConnected = false)
                    Timber.Forest.i(
                        "%s disconnectedByName host=%s service=%s summary=%s",
                        DIAG_PREFIX,
                        hostAddress,
                        hostName,
                        debugSummaryLocked(nowMillis)
                    )
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
                Timber.Forest.i(
                    "%s hostInfo host=%s name=%s summary=%s",
                    DIAG_PREFIX,
                    hostAddress,
                    name,
                    debugSummaryLocked()
                )
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
                Timber.Forest.i(
                    "%s heartbeatRx host=%s connected=%s summary=%s",
                    DIAG_PREFIX,
                    hostAddress,
                    updated.isConnected,
                    debugSummaryLocked(now)
                )
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
                    Timber.Forest.i(
                        "%s staleDisconnect host=%s silenceMs=%d maxMs=%d",
                        DIAG_PREFIX,
                        hostAddress,
                        nowMillis - client.lastDataReceivedAt,
                        maxSilenceMs
                    )
                    changed = true
                }
            }
            if (changed) {
                Timber.Forest.i(
                    "%s staleSweep summary=%s",
                    DIAG_PREFIX,
                    debugSummaryLocked(nowMillis)
                )
                publishChangesLocked()
            }
        }
    }

    fun clearAll() {
        synchronized(lock) {
            if (clients.isNotEmpty()) {
                Timber.Forest.i("%s clearAll previous=%s", DIAG_PREFIX, debugSummaryLocked())
            }
            clients.clear()
            publishChangesLocked()
        }
    }

    fun debugSummary(nowMillis: Long = Date().time): String {
        synchronized(lock) {
            return debugSummaryLocked(nowMillis)
        }
    }

    private fun debugSummaryLocked(nowMillis: Long = Date().time): String {
        val connected = clients.values.count { it.isConnected }
        val entries = clients.values.sortedBy { it.hostAddress }.joinToString(separator = ";") { client ->
            val age = if (client.lastDataReceivedAt > 0L) {
                (nowMillis - client.lastDataReceivedAt).coerceAtLeast(0L)
            } else {
                -1L
            }
            "${client.hostAddress}|c=${client.isConnected}|p=${client.port}|ageMs=$age|n=${client.hostName}"
        }
        return "connected=$connected total=${clients.size} [$entries]"
    }
}

private const val DISCONNECT_DEBOUNCE_MS = 2_500L
private const val DIAG_PREFIX = "[DIAG_CONN]"
