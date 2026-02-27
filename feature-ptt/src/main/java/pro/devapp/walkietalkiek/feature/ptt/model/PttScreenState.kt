package pro.devapp.walkietalkiek.feature.ptt.model

import pro.devapp.walkietalkiek.serivce.network.data.model.ClientModel

internal data class PttScreenState(
    val isConnected: Boolean,
    val myIP: String,
    val connectedDevices: List<ClientModel>,
    val voiceData: ByteArray? = null,
    val isRecording: Boolean = false,
    val isRemoteSpeaking: Boolean = false,
    val isFloorRequestPending: Boolean = false,
    val isFloorHeldByMe: Boolean = false,
    val floorOwnerHostAddress: String? = null,
    val selfNodeId: String = "",
    val controlPlaneLabel: String = "Serverless",
    val controlPlaneDetail: String = "LAN P2P (NSD + Sockets)",
    val clusterRoleLabel: String = "Peer",
    val leaderNodeLabel: String = "--",
    val clusterMembersCount: Int = 1,
    val talkDurationSeconds: Int = 10,
    val remainingTalkSeconds: Int = 10,
    val remainingTalkMillis: Long = 10_000L
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PttScreenState

        if (isConnected != other.isConnected) return false
        if (myIP != other.myIP) return false
        if (connectedDevices != other.connectedDevices) return false
        if (!voiceData.contentEquals(other.voiceData)) return false
        if (isRecording != other.isRecording) return false
        if (isRemoteSpeaking != other.isRemoteSpeaking) return false
        if (isFloorRequestPending != other.isFloorRequestPending) return false
        if (isFloorHeldByMe != other.isFloorHeldByMe) return false
        if (floorOwnerHostAddress != other.floorOwnerHostAddress) return false
        if (selfNodeId != other.selfNodeId) return false
        if (controlPlaneLabel != other.controlPlaneLabel) return false
        if (controlPlaneDetail != other.controlPlaneDetail) return false
        if (clusterRoleLabel != other.clusterRoleLabel) return false
        if (leaderNodeLabel != other.leaderNodeLabel) return false
        if (clusterMembersCount != other.clusterMembersCount) return false
        if (talkDurationSeconds != other.talkDurationSeconds) return false
        if (remainingTalkSeconds != other.remainingTalkSeconds) return false
        if (remainingTalkMillis != other.remainingTalkMillis) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isConnected.hashCode()
        result = 31 * result + myIP.hashCode()
        result = 31 * result + connectedDevices.hashCode()
        result = 31 * result + (voiceData?.contentHashCode() ?: 0)
        result = 31 * result + isRecording.hashCode()
        result = 31 * result + isRemoteSpeaking.hashCode()
        result = 31 * result + isFloorRequestPending.hashCode()
        result = 31 * result + isFloorHeldByMe.hashCode()
        result = 31 * result + (floorOwnerHostAddress?.hashCode() ?: 0)
        result = 31 * result + selfNodeId.hashCode()
        result = 31 * result + controlPlaneLabel.hashCode()
        result = 31 * result + controlPlaneDetail.hashCode()
        result = 31 * result + clusterRoleLabel.hashCode()
        result = 31 * result + leaderNodeLabel.hashCode()
        result = 31 * result + clusterMembersCount
        result = 31 * result + talkDurationSeconds
        result = 31 * result + remainingTalkSeconds
        result = 31 * result + remainingTalkMillis.hashCode()
        return result
    }
}
