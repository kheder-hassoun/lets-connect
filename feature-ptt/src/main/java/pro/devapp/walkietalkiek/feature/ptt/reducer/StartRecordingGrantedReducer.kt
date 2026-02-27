package pro.devapp.walkietalkiek.feature.ptt.reducer

import pro.devapp.walkietalkiek.core.mvi.Reducer
import pro.devapp.walkietalkiek.feature.ptt.model.PttAction
import pro.devapp.walkietalkiek.feature.ptt.model.PttEvent
import pro.devapp.walkietalkiek.feature.ptt.model.PttScreenState
import pro.devapp.walkietalkiek.service.voice.VoiceRecorder

internal class StartRecordingGrantedReducer(
    private val voiceRecorder: VoiceRecorder
) : Reducer<PttAction.StartRecordingGranted, PttScreenState, PttAction, PttEvent> {

    override val actionClass = PttAction.StartRecordingGranted::class

    override suspend fun reduce(
        action: PttAction.StartRecordingGranted,
        getState: () -> PttScreenState
    ): Reducer.Result<PttScreenState, PttAction, PttEvent?> {
        val state = getState()
        if (state.isRecording) {
            return Reducer.Result(state = state, event = null)
        }
        voiceRecorder.startRecord()
        return Reducer.Result(
            state = state.copy(
                isRecording = true,
                isRemoteSpeaking = false,
                isFloorHeldByMe = true,
                isFloorRequestPending = false,
                voiceData = null,
                remainingTalkSeconds = state.talkDurationSeconds,
                remainingTalkMillis = state.talkDurationSeconds * 1000L
            ),
            event = null
        )
    }
}
