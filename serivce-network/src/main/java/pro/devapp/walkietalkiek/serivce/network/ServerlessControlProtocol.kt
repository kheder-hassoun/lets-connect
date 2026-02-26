package pro.devapp.walkietalkiek.serivce.network

object ServerlessControlProtocol {
    private const val PREFIX = "__CTRL2__:"
    private const val HEARTBEAT = "HEARTBEAT"
    private const val FLOOR_REQUEST = "FLOOR_REQUEST"
    private const val FLOOR_GRANT = "FLOOR_GRANT"
    private const val FLOOR_RELEASE = "FLOOR_RELEASE"
    private const val FLOOR_BUSY = "FLOOR_BUSY"

    fun heartbeatPacket(
        nodeId: String,
        term: Long,
        seq: Long,
        timestampMs: Long
    ): ByteArray {
        return "$PREFIX$HEARTBEAT|$nodeId|$term|$seq|$timestampMs".toByteArray()
    }

    fun floorRequestPacket(
        nodeId: String,
        term: Long,
        seq: Long,
        timestampMs: Long
    ): ByteArray {
        return "$PREFIX$FLOOR_REQUEST|$nodeId|$term|$seq|$timestampMs".toByteArray()
    }

    fun floorGrantPacket(
        leaderNodeId: String,
        targetNodeId: String,
        term: Long,
        seq: Long,
        timestampMs: Long
    ): ByteArray {
        return "$PREFIX$FLOOR_GRANT|$leaderNodeId|$term|$seq|$timestampMs|$targetNodeId".toByteArray()
    }

    fun floorReleasePacket(
        nodeId: String,
        term: Long,
        seq: Long,
        timestampMs: Long
    ): ByteArray {
        return "$PREFIX$FLOOR_RELEASE|$nodeId|$term|$seq|$timestampMs".toByteArray()
    }

    fun floorBusyPacket(
        leaderNodeId: String,
        ownerNodeId: String,
        term: Long,
        seq: Long,
        timestampMs: Long
    ): ByteArray {
        return "$PREFIX$FLOOR_BUSY|$leaderNodeId|$term|$seq|$timestampMs|$ownerNodeId".toByteArray()
    }

    fun parse(message: String): ControlEnvelope? {
        val normalized = message.trim()
        if (!normalized.startsWith(PREFIX)) return null
        val body = normalized.removePrefix(PREFIX)
        val parts = body.split('|')
        if (parts.size < 5) return null
        val type = parts[0]
        val nodeId = parts[1]
        val term = parts[2].toLongOrNull() ?: return null
        val seq = parts[3].toLongOrNull() ?: return null
        val timestampMs = parts[4].toLongOrNull() ?: return null
        return when (type) {
            HEARTBEAT -> ControlEnvelope.Heartbeat(nodeId, term, seq, timestampMs)
            FLOOR_REQUEST -> ControlEnvelope.FloorRequest(nodeId, term, seq, timestampMs)
            FLOOR_GRANT -> {
                val targetNodeId = parts.getOrNull(5)?.trim().orEmpty()
                if (targetNodeId.isBlank()) return null
                ControlEnvelope.FloorGrant(
                    nodeId = nodeId,
                    targetNodeId = targetNodeId,
                    term = term,
                    seq = seq,
                    timestampMs = timestampMs
                )
            }
            FLOOR_RELEASE -> ControlEnvelope.FloorRelease(nodeId, term, seq, timestampMs)
            FLOOR_BUSY -> {
                val ownerNodeId = parts.getOrNull(5)?.trim().orEmpty()
                if (ownerNodeId.isBlank()) return null
                ControlEnvelope.FloorBusy(
                    nodeId = nodeId,
                    ownerNodeId = ownerNodeId,
                    term = term,
                    seq = seq,
                    timestampMs = timestampMs
                )
            }
            else -> null
        }
    }
}

sealed interface ControlEnvelope {
    val nodeId: String
    val term: Long
    val seq: Long
    val timestampMs: Long

    data class Heartbeat(
        override val nodeId: String,
        override val term: Long,
        override val seq: Long,
        override val timestampMs: Long
    ) : ControlEnvelope

    data class FloorRequest(
        override val nodeId: String,
        override val term: Long,
        override val seq: Long,
        override val timestampMs: Long
    ) : ControlEnvelope

    data class FloorGrant(
        override val nodeId: String,
        val targetNodeId: String,
        override val term: Long,
        override val seq: Long,
        override val timestampMs: Long
    ) : ControlEnvelope

    data class FloorRelease(
        override val nodeId: String,
        override val term: Long,
        override val seq: Long,
        override val timestampMs: Long
    ) : ControlEnvelope

    data class FloorBusy(
        override val nodeId: String,
        val ownerNodeId: String,
        override val term: Long,
        override val seq: Long,
        override val timestampMs: Long
    ) : ControlEnvelope
}
