package pro.devapp.walkietalkiek.feature.ptt.reducer

import pro.devapp.walkietalkiek.core.mvi.Reducer
import pro.devapp.walkietalkiek.feature.ptt.model.PttAction
import pro.devapp.walkietalkiek.feature.ptt.model.PttEvent
import pro.devapp.walkietalkiek.feature.ptt.model.PttScreenState
import pro.devapp.walkietalkiek.service.voice.VoiceRecorder

internal class StartRecordingReducer(
    private val voiceRecorder: VoiceRecorder
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
        voiceRecorder.startRecord()
        return Reducer.Result(
            state = state.copy(
                isRecording = true,
                remainingTalkSeconds = state.talkDurationSeconds,
                remainingTalkMillis = state.talkDurationSeconds * 1000L
            ),
            event = null
        )
    }

}
