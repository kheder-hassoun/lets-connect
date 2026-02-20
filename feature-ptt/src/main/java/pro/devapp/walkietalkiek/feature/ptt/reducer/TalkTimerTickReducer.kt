package pro.devapp.walkietalkiek.feature.ptt.reducer

import pro.devapp.walkietalkiek.core.mvi.Reducer
import pro.devapp.walkietalkiek.feature.ptt.model.PttAction
import pro.devapp.walkietalkiek.feature.ptt.model.PttEvent
import pro.devapp.walkietalkiek.feature.ptt.model.PttScreenState

internal class TalkTimerTickReducer :
    Reducer<PttAction.TalkTimerTick, PttScreenState, PttAction, PttEvent> {

    override val actionClass = PttAction.TalkTimerTick::class

    override suspend fun reduce(
        action: PttAction.TalkTimerTick,
        getState: () -> PttScreenState
    ): Reducer.Result<PttScreenState, PttAction, PttEvent?> {
        return Reducer.Result(
            state = getState().copy(
                remainingTalkSeconds = action.remainingSeconds.coerceAtLeast(0)
            ),
            event = null
        )
    }
}

