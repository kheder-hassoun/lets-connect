package pro.devapp.walkietalkiek.feature.ptt.reducer

import pro.devapp.walkietalkiek.core.mvi.Reducer
import pro.devapp.walkietalkiek.feature.ptt.model.PttAction
import pro.devapp.walkietalkiek.feature.ptt.model.PttEvent
import pro.devapp.walkietalkiek.feature.ptt.model.PttScreenState

internal class ClearVoiceVisualizationReducer :
    Reducer<PttAction.ClearVoiceVisualization, PttScreenState, PttAction, PttEvent> {

    override val actionClass = PttAction.ClearVoiceVisualization::class

    override suspend fun reduce(
        action: PttAction.ClearVoiceVisualization,
        getState: () -> PttScreenState
    ): Reducer.Result<PttScreenState, PttAction, PttEvent?> {
        val state = getState()
        if (state.voiceData == null && !state.isRemoteSpeaking) {
            return Reducer.Result(state = state, event = null)
        }
        return Reducer.Result(
            state = state.copy(
                voiceData = null,
                isRemoteSpeaking = false
            ),
            event = null
        )
    }
}
