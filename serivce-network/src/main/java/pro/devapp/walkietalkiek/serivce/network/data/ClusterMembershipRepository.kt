package pro.devapp.walkietalkiek.serivce.network.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
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
        val sessionStartedAtMs: Long,
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
            sessionStartedAtMs = existing?.sessionStartedAtMs ?: nowMs,
            firstSeenMs = existing?.firstSeenMs ?: nowMs,
            lastSeenMs = nowMs,
            term = _status.value.term,
            uptimeMs = existing?.uptimeMs ?: 0L
        )
        if (_status.value.selfNodeId != nodeId) {
            _status.value = _status.value.copy(
                selfNodeId = nodeId
            )
            Timber.Forest.i("%s initializeSelf node=%s", DIAG_PREFIX, nodeId)
        }
        recalculateLeader(nowMs)
    }

    fun nextSequence(): Long {
        localSeq += 1L
        return localSeq
    }

    fun onHeartbeat(nodeId: String, term: Long, timestampMs: Long, nowMs: Long) {
        val seenAt = nowMs
        val existing = members[nodeId]
        val sessionStartedAtMs = existing?.sessionStartedAtMs ?: seenAt
        if (existing == null) {
            Timber.Forest.i("%s heartbeat newMember node=%s term=%d", DIAG_PREFIX, nodeId, term)
        }
        members[nodeId] = MemberState(
            nodeId = nodeId,
            sessionStartedAtMs = sessionStartedAtMs,
            firstSeenMs = existing?.firstSeenMs ?: sessionStartedAtMs,
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
        val seenAt = nowMs
        val existing = members[nodeId]
        val isNewSession = existing == null || existing.sessionStartedAtMs != joinedAtMs
        if (isNewSession) {
            Timber.Forest.i(
                "%s heartbeat sessionChange node=%s oldSession=%d newSession=%d",
                DIAG_PREFIX,
                nodeId,
                existing?.sessionStartedAtMs ?: -1L,
                joinedAtMs
            )
        }
        val firstSeenMs = if (isNewSession) seenAt else (existing?.firstSeenMs ?: seenAt)
        members[nodeId] = MemberState(
            nodeId = nodeId,
            sessionStartedAtMs = joinedAtMs,
            firstSeenMs = firstSeenMs,
            lastSeenMs = seenAt,
            term = term,
            uptimeMs = if (isNewSession) 0L else (existing?.uptimeMs ?: 0L)
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
        val seenAt = nowMs
        val existing = members[nodeId]
        val isNewSession = existing == null || existing.sessionStartedAtMs != joinedAtMs
        if (isNewSession) {
            Timber.Forest.i(
                "%s heartbeat sessionChange+uptime node=%s oldSession=%d newSession=%d uptimeMs=%d",
                DIAG_PREFIX,
                nodeId,
                existing?.sessionStartedAtMs ?: -1L,
                joinedAtMs,
                uptimeMs
            )
        }
        val firstSeenMs = if (isNewSession) seenAt else (existing?.firstSeenMs ?: seenAt)
        val computedUptime = if (isNewSession) {
            uptimeMs.coerceAtLeast(0L)
        } else {
            uptimeMs.coerceAtLeast(existing?.uptimeMs ?: 0L)
        }
        members[nodeId] = MemberState(
            nodeId = nodeId,
            sessionStartedAtMs = joinedAtMs,
            firstSeenMs = firstSeenMs,
            lastSeenMs = seenAt,
            term = term,
            uptimeMs = computedUptime
        )
        val nextTerm = maxOf(_status.value.term, term)
        if (nextTerm != _status.value.term) {
            _status.value = _status.value.copy(term = nextTerm)
        }
        recalculateLeader(nowMs)
    }

    fun sweepStale(staleTimeoutMs: Long, nowMs: Long) {
        val removed = mutableListOf<String>()
        members.entries.removeIf { (_, member) ->
            val shouldRemove = nowMs - member.lastSeenMs > staleTimeoutMs
            if (shouldRemove) {
                removed += member.nodeId
            }
            shouldRemove
        }
        if (removed.isNotEmpty()) {
            Timber.Forest.i("%s staleMembers removed=%s timeoutMs=%d", DIAG_PREFIX, removed, staleTimeoutMs)
        }
        recalculateLeader(nowMs)
    }

    fun clear() {
        if (members.isNotEmpty()) {
            Timber.Forest.i("%s clear members=%s", DIAG_PREFIX, members.keys.joinToString(","))
        }
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
                sessionStartedAtMs = nowMs,
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
        val previous = _status.value
        val next = previous.copy(
            leaderNodeId = leader,
            role = role,
            activeMembersCount = uniqueMembers.size.coerceAtLeast(1)
        )
        _status.value = next
        if (previous.leaderNodeId != next.leaderNodeId ||
            previous.role != next.role ||
            previous.activeMembersCount != next.activeMembersCount
        ) {
            Timber.Forest.i(
                "%s statusChange self=%s leader=%s role=%s members=%d active=%s",
                DIAG_PREFIX,
                next.selfNodeId,
                next.leaderNodeId,
                next.role,
                next.activeMembersCount,
                uniqueMembers.joinToString(",") { it.nodeId }
            )
        }
    }
}

private const val ACTIVE_WINDOW_MS = 6_000L
private const val DIAG_PREFIX = "[DIAG_CLUSTER]"
