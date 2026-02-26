package pro.devapp.walkietalkiek.feature.ptt.reducer

import pro.devapp.walkietalkiek.core.mvi.Reducer
import pro.devapp.walkietalkiek.core.flags.FeatureFlagsRepository
import pro.devapp.walkietalkiek.feature.ptt.model.PttAction
import pro.devapp.walkietalkiek.feature.ptt.model.PttEvent
import pro.devapp.walkietalkiek.feature.ptt.model.PttScreenState
import pro.devapp.walkietalkiek.serivce.network.FloorControlProtocol
import pro.devapp.walkietalkiek.serivce.network.FloorLeaseController
import pro.devapp.walkietalkiek.serivce.network.FloorLeaseRequestResult
import pro.devapp.walkietalkiek.serivce.network.MessageController
import pro.devapp.walkietalkiek.service.voice.VoiceRecorder

internal class StartRecordingReducer(
    private val voiceRecorder: VoiceRecorder,
    private val messageController: MessageController,
    private val floorLeaseController: FloorLeaseController,
    private val featureFlagsRepository: FeatureFlagsRepository
)
    : Reducer<PttAction.StartRecording, PttScreenState, PttAction, PttEvent> {

    override val actionClass = PttAction.StartRecording::class

    override suspend fun reduce(
        action: PttAction.StartRecording,
        getState: () -> PttScreenState
    ): Reducer.Result<PttScreenState, PttAction, PttEvent?> {
        val state = getState()
        if (state.isRecording) {
            return Reducer.Result(
                state = state,
                event = null
            )
        }
        if (featureFlagsRepository.flags.value.serverlessControl) {
            return when (floorLeaseController.requestFloor()) {
                FloorLeaseRequestResult.Granted -> {
                    voiceRecorder.startRecord()
                    Reducer.Result(
                        state = state.copy(
                            isRecording = true,
                            isFloorRequestPending = false,
                            isFloorHeldByMe = true,
                            floorOwnerHostAddress = state.selfNodeId.ifBlank { state.myIP }.let { "node:$it" },
                            remainingTalkSeconds = state.talkDurationSeconds,
                            remainingTalkMillis = state.talkDurationSeconds * 1000L
                        ),
                        event = null
                    )
                }
                FloorLeaseRequestResult.Pending -> Reducer.Result(
                    state = state.copy(
                        isFloorRequestPending = true
                    ),
                    event = null
                )
            }
        }
        val isFloorBusyByRemote = state.floorOwnerHostAddress != null && !state.isFloorHeldByMe
        if (isFloorBusyByRemote) {
            return Reducer.Result(
                state = state,
                event = null
            )
        }
        messageController.sendMessage(FloorControlProtocol.acquirePacket())
        voiceRecorder.startRecord()
        return Reducer.Result(
            state = state.copy(
                isRecording = true,
                isFloorRequestPending = false,
                isFloorHeldByMe = true,
                floorOwnerHostAddress = state.myIP,
                remainingTalkSeconds = state.talkDurationSeconds,
                remainingTalkMillis = state.talkDurationSeconds * 1000L
            ),
            event = null
        )
    }

}
