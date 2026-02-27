package pro.devapp.walkietalkiek.feature.ptt.reducer

import pro.devapp.walkietalkiek.core.mvi.Reducer
import pro.devapp.walkietalkiek.feature.ptt.model.PttAction
import pro.devapp.walkietalkiek.feature.ptt.model.PttEvent
import pro.devapp.walkietalkiek.feature.ptt.model.PttScreenState

internal class RemoteSpeakingChangedReducer :
    Reducer<PttAction.RemoteSpeakingChanged, PttScreenState, PttAction, PttEvent> {

    override val actionClass = PttAction.RemoteSpeakingChanged::class

    override suspend fun reduce(
        action: PttAction.RemoteSpeakingChanged,
        getState: () -> PttScreenState
    ): Reducer.Result<PttScreenState, PttAction, PttEvent?> {
        val state = getState()
        val shouldSpeak = action.isSpeaking && !state.isRecording && !state.isFloorHeldByMe
        if (state.isRemoteSpeaking == shouldSpeak) {
            return Reducer.Result(state = state, event = null)
        }
        return Reducer.Result(
            state = state.copy(isRemoteSpeaking = shouldSpeak),
            event = null
        )
    }
}
