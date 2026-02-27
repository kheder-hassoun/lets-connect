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
        val firstSeenMs: Long,
        val lastSeenMs: Long,
        val term: Long,
        val uptimeMs: Long
    )

    private val members = ConcurrentHashMap<String, MemberState>()
    private val _status = MutableStateFlow(ClusterStatus())
    val status: StateFlow<ClusterStatus> = _status.asStateFlow()

    @Volatile
    private var localSeq: Long = 0L

    fun initializeSelf(nodeId: String, nowMs: Long) {
        val existing = members[nodeId]
        members[nodeId] = MemberState(
            nodeId = nodeId,
            firstSeenMs = existing?.firstSeenMs ?: nowMs,
            lastSeenMs = nowMs,
            term = _status.value.term,
            uptimeMs = existing?.uptimeMs ?: 0L
        )
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
        val existing = members[nodeId]
        members[nodeId] = MemberState(
            nodeId = nodeId,
            firstSeenMs = existing?.firstSeenMs ?: seenAt,
            lastSeenMs = seenAt,
            term = term,
            uptimeMs = existing?.uptimeMs ?: 0L
        )
        val nextTerm = maxOf(_status.value.term, term)
        if (nextTerm != _status.value.term) {
            _status.value = _status.value.copy(term = nextTerm)
        }
        recalculateLeader(nowMs)
    }

    fun onHeartbeat(
        nodeId: String,
        term: Long,
        timestampMs: Long,
        nowMs: Long,
        joinedAtMs: Long
    ) {
        val seenAt = maxOf(nowMs, timestampMs)
        val existing = members[nodeId]
        members[nodeId] = MemberState(
            nodeId = nodeId,
            firstSeenMs = existing?.firstSeenMs ?: joinedAtMs,
            lastSeenMs = seenAt,
            term = term,
            uptimeMs = existing?.uptimeMs ?: 0L
        )
        val nextTerm = maxOf(_status.value.term, term)
        if (nextTerm != _status.value.term) {
            _status.value = _status.value.copy(term = nextTerm)
        }
        recalculateLeader(nowMs)
    }

    fun onHeartbeat(
        nodeId: String,
        term: Long,
        timestampMs: Long,
        nowMs: Long,
        joinedAtMs: Long,
        uptimeMs: Long
    ) {
        val seenAt = maxOf(nowMs, timestampMs)
        val existing = members[nodeId]
        members[nodeId] = MemberState(
            nodeId = nodeId,
            firstSeenMs = existing?.firstSeenMs ?: joinedAtMs,
            lastSeenMs = seenAt,
            term = term,
            uptimeMs = uptimeMs.coerceAtLeast(existing?.uptimeMs ?: 0L)
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
        val selfNodeId = _status.value.selfNodeId
        val withSelf = if (
            selfNodeId.isNotBlank() &&
            activeMembers.none { it.nodeId == selfNodeId }
        ) {
            activeMembers + MemberState(
                nodeId = selfNodeId,
                firstSeenMs = nowMs,
                lastSeenMs = nowMs,
                term = _status.value.term,
                uptimeMs = 0L
            )
        } else {
            activeMembers
        }
        val uniqueMembers = withSelf
            .filter { it.nodeId.isNotBlank() }
            .distinctBy { it.nodeId }
        val leader = uniqueMembers
            .sortedWith(
                compareByDescending<MemberState> { it.uptimeMs }
                    .thenBy { it.firstSeenMs }
                    .thenBy { it.nodeId }
            )
            .firstOrNull()
            ?.nodeId
            .orEmpty()
        val role = if (leader.isNotBlank() && leader == selfNodeId) {
            ClusterRole.LEADER
        } else {
            ClusterRole.PEER
        }
        _status.value = _status.value.copy(
            leaderNodeId = leader,
            role = role,
            activeMembersCount = uniqueMembers.size.coerceAtLeast(1)
        )
    }
}

private const val ACTIVE_WINDOW_MS = 20_000L
