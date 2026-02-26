package pro.devapp.walkietalkiek.serivce.network.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

data class ClusterStatus(
    val selfNodeId: String = "",
    val leaderNodeId: String = "",
    val role: ClusterRole = ClusterRole.PEER,
    val activeMembersCount: Int = 1,
    val term: Long = 0L
)

enum class ClusterRole {
    LEADER,
    PEER
}

class ClusterMembershipRepository {
    private data class MemberState(
        val nodeId: String,
        val lastSeenMs: Long,
        val term: Long
    )

    private val members = ConcurrentHashMap<String, MemberState>()
    private val _status = MutableStateFlow(ClusterStatus())
    val status: StateFlow<ClusterStatus> = _status.asStateFlow()

    @Volatile
    private var localSeq: Long = 0L

    fun initializeSelf(nodeId: String, nowMs: Long) {
        members[nodeId] = MemberState(nodeId = nodeId, lastSeenMs = nowMs, term = _status.value.term)
        if (_status.value.selfNodeId != nodeId) {
            _status.value = _status.value.copy(
                selfNodeId = nodeId
            )
        }
        recalculateLeader(nowMs)
    }

    fun nextSequence(): Long {
        localSeq += 1L
        return localSeq
    }

    fun onHeartbeat(nodeId: String, term: Long, timestampMs: Long, nowMs: Long) {
        val seenAt = maxOf(nowMs, timestampMs)
        members[nodeId] = MemberState(
            nodeId = nodeId,
            lastSeenMs = seenAt,
            term = term
        )
        val nextTerm = maxOf(_status.value.term, term)
        if (nextTerm != _status.value.term) {
            _status.value = _status.value.copy(term = nextTerm)
        }
        recalculateLeader(nowMs)
    }

    fun sweepStale(staleTimeoutMs: Long, nowMs: Long) {
        members.entries.removeIf { (_, member) ->
            nowMs - member.lastSeenMs > staleTimeoutMs
        }
        recalculateLeader(nowMs)
    }

    fun clear() {
        members.clear()
        localSeq = 0L
        _status.value = ClusterStatus()
    }

    private fun recalculateLeader(nowMs: Long) {
        val activeMembers = members.values
            .filter { member -> nowMs - member.lastSeenMs <= ACTIVE_WINDOW_MS }
            .map { it.nodeId }
            .distinct()
        val selfNodeId = _status.value.selfNodeId
        val allNodes = (activeMembers + listOfNotNull(selfNodeId))
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        val leader = allNodes.lastOrNull().orEmpty()
        val role = if (leader.isNotBlank() && leader == selfNodeId) {
            ClusterRole.LEADER
        } else {
            ClusterRole.PEER
        }
        _status.value = _status.value.copy(
            leaderNodeId = leader,
            role = role,
            activeMembersCount = allNodes.size.coerceAtLeast(1)
        )
    }
}

private const val ACTIVE_WINDOW_MS = 20_000L
