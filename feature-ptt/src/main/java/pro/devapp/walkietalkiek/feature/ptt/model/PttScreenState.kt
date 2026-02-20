package pro.devapp.walkietalkiek.feature.ptt.model

import pro.devapp.walkietalkiek.serivce.network.data.model.ClientModel

internal data class PttScreenState(
    val isConnected: Boolean,
    val myIP: String,
    val connectedDevices: List<ClientModel>,
    val voiceData: ByteArray? = null,
    val isRecording: Boolean = false,
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
        result = 31 * result + talkDurationSeconds
        result = 31 * result + remainingTalkSeconds
        result = 31 * result + remainingTalkMillis.hashCode()
        return result
    }
}
