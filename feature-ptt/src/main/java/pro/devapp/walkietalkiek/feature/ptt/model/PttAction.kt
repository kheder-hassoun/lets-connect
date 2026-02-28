package pro.devapp.walkietalkiek.feature.ptt.model

import pro.devapp.walkietalkiek.serivce.network.data.model.ClientModel

internal sealed interface PttAction {

    data object InitScreen : PttAction
    data class ConnectedDevicesUpdated(
        val connectedDevices: List<ClientModel>
    ): PttAction
    data object StartRecording : PttAction
    data object StartRecordingGranted : PttAction
    data object StopRecording : PttAction
    data class FloorOwnerChanged(val ownerHostAddress: String?) : PttAction
    data class ClusterStatusChanged(
        val selfNodeId: String,
        val roleLabel: String,
        val leaderNodeLabel: String,
        val membersCount: Int
    ) : PttAction
    data class ClusterStabilizationChanged(
        val isVisible: Boolean,
        val title: String,
        val detail: String
    ) : PttAction
    data class TalkTimerTick(val remainingMillis: Long) : PttAction
    data class TalkDurationChanged(val seconds: Int) : PttAction
    data class RemoteSpeakingChanged(val isSpeaking: Boolean) : PttAction
    data object ClearVoiceVisualization : PttAction
    data class VoiceDataReceived(
        val voiceData: ByteArray
    ): PttAction {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as VoiceDataReceived

            if (!voiceData.contentEquals(other.voiceData)) return false

            return true
        }

        override fun hashCode(): Int {
            return voiceData.contentHashCode()
        }
    }
}
