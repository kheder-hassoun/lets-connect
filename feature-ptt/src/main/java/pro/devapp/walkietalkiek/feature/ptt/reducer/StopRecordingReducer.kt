package pro.devapp.walkietalkiek.feature.ptt.reducer

import pro.devapp.walkietalkiek.core.mvi.Reducer
import pro.devapp.walkietalkiek.core.flags.FeatureFlagsRepository
import pro.devapp.walkietalkiek.feature.ptt.model.PttAction
import pro.devapp.walkietalkiek.feature.ptt.model.PttEvent
import pro.devapp.walkietalkiek.feature.ptt.model.PttScreenState
import pro.devapp.walkietalkiek.serivce.network.FloorControlProtocol
import pro.devapp.walkietalkiek.serivce.network.FloorLeaseController
import pro.devapp.walkietalkiek.serivce.network.MessageController
import pro.devapp.walkietalkiek.service.voice.VoiceRecorder

internal class StopRecordingReducer(
    private val voiceRecorder: VoiceRecorder,
    private val messageController: MessageController,
    private val floorLeaseController: FloorLeaseController,
    private val featureFlagsRepository: FeatureFlagsRepository
)
    : Reducer<PttAction.StopRecording, PttScreenState, PttAction, PttEvent> {

    override val actionClass = PttAction.StopRecording::class

    override suspend fun reduce(
        action: PttAction.StopRecording,
        getState: () -> PttScreenState
    ): Reducer.Result<PttScreenState, PttAction, PttEvent?> {
        val state = getState()
        if (!state.isRecording && !state.isFloorRequestPending) {
            return Reducer.Result(
                state = state,
                event = null
            )
        }
        if (state.isRecording) {
            voiceRecorder.stopRecord()
        }
        if (state.isFloorHeldByMe || state.isFloorRequestPending) {
            if (featureFlagsRepository.flags.value.serverlessControl) {
                floorLeaseController.releaseFloor()
            } else {
                messageController.sendMessage(FloorControlProtocol.releasePacket())
            }
        }
        return Reducer.Result(
            state = state.copy(
                isRecording = false,
                isRemoteSpeaking = false,
                isFloorRequestPending = false,
                isFloorHeldByMe = false,
                floorOwnerHostAddress = null,
                voiceData = null,
                remainingTalkSeconds = state.talkDurationSeconds,
                remainingTalkMillis = state.talkDurationSeconds * 1000L
            ),
            event = null
        )
    }

}
