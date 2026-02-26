package pro.devapp.walkietalkiek.serivce.network

data class FloorQueueTarget(
    val nodeId: String,
    val hostAddress: String
)

class FloorArbitrationState {
    private val lock = Any()
    private val nodeHostMap = HashMap<String, String>()
    private val pendingFloorQueue = ArrayDeque<String>()

    fun rememberNodeHost(nodeId: String, hostAddress: String) {
        if (nodeId.isBlank() || hostAddress.isBlank()) return
        synchronized(lock) {
            nodeHostMap[nodeId] = hostAddress
        }
    }

    fun removeHost(hostAddress: String): List<String> {
        if (hostAddress.isBlank()) return emptyList()
        synchronized(lock) {
            val removedNodeIds = nodeHostMap.entries
                .filter { it.value == hostAddress }
                .map { it.key }
            if (removedNodeIds.isEmpty()) {
                return emptyList()
            }
            removedNodeIds.forEach { nodeId ->
                nodeHostMap.remove(nodeId)
                pendingFloorQueue.remove(nodeId)
            }
            return removedNodeIds
        }
    }

    fun removeNodeFromQueue(nodeId: String) {
        if (nodeId.isBlank()) return
        synchronized(lock) {
            pendingFloorQueue.remove(nodeId)
        }
    }

    fun enqueue(nodeId: String) {
        if (nodeId.isBlank()) return
        synchronized(lock) {
            if (!pendingFloorQueue.contains(nodeId)) {
                pendingFloorQueue.addLast(nodeId)
            }
        }
    }

    fun pollNextTarget(): FloorQueueTarget? {
        synchronized(lock) {
            while (pendingFloorQueue.isNotEmpty()) {
                val nextNodeId = pendingFloorQueue.removeFirst()
                val nextHostAddress = nodeHostMap[nextNodeId]
                if (!nextHostAddress.isNullOrBlank()) {
                    return FloorQueueTarget(
                        nodeId = nextNodeId,
                        hostAddress = nextHostAddress
                    )
                }
            }
            return null
        }
    }

    fun clear() {
        synchronized(lock) {
            pendingFloorQueue.clear()
            nodeHostMap.clear()
        }
    }
}
