package pro.devapp.walkietalkiek.serivce.network.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pro.devapp.walkietalkiek.core.mvi.CoroutineContextProvider
import pro.devapp.walkietalkiek.core.settings.AppSettingsRepository

class PttFloorRepository(
    private val appSettingsRepository: AppSettingsRepository,
    private val coroutineContextProvider: CoroutineContextProvider
) {
    private val lock = Any()
    private val scope = coroutineContextProvider.createScope(coroutineContextProvider.default)
    private val _currentFloorOwnerHost = MutableStateFlow<String?>(null)
    val currentFloorOwnerHost: StateFlow<String?>
        get() = _currentFloorOwnerHost
    private var floorSessionVersion: Long = 0L

    fun acquire(hostAddress: String) {
        val fallbackMs = appSettingsRepository.settings.value.talkDurationSeconds
            .coerceAtLeast(1) * 1000L
        val version: Long
        synchronized(lock) {
            _currentFloorOwnerHost.value = hostAddress
            floorSessionVersion += 1
            version = floorSessionVersion
        }
        scheduleAutoRelease(
            hostAddress = hostAddress,
            version = version,
            fallbackMs = fallbackMs
        )
    }

    fun release(hostAddress: String) {
        releaseIfOwner(hostAddress)
    }

    fun releaseIfOwner(hostAddress: String): Boolean {
        synchronized(lock) {
            if (_currentFloorOwnerHost.value == hostAddress) {
                _currentFloorOwnerHost.value = null
                floorSessionVersion += 1
                return true
            }
        }
        return false
    }

    fun releaseByNodeId(nodeId: String): Boolean {
        val normalizedNodeId = nodeId.trim()
        if (normalizedNodeId.isBlank()) return false
        synchronized(lock) {
            val currentOwner = _currentFloorOwnerHost.value ?: return false
            val ownerNodeId = currentOwner.removePrefix("node:").trim()
            if (ownerNodeId.equals(normalizedNodeId, ignoreCase = true)) {
                _currentFloorOwnerHost.value = null
                floorSessionVersion += 1
                return true
            }
        }
        return false
    }

    fun clear() {
        synchronized(lock) {
            _currentFloorOwnerHost.value = null
            floorSessionVersion += 1
        }
    }

    private fun scheduleAutoRelease(hostAddress: String, version: Long, fallbackMs: Long) {
        scope.launch {
            delay(fallbackMs)
            synchronized(lock) {
                val isSameSession = floorSessionVersion == version
                val stillOwnedBySender = _currentFloorOwnerHost.value == hostAddress
                if (isSameSession && stillOwnedBySender) {
                    _currentFloorOwnerHost.value = null
                    floorSessionVersion += 1
                }
            }
        }
    }
}
