package pro.devapp.walkietalkiek.feature.ptt.reducer

import pro.devapp.walkietalkiek.core.mvi.Reducer
import pro.devapp.walkietalkiek.feature.ptt.model.PttAction
import pro.devapp.walkietalkiek.feature.ptt.model.PttEvent
import pro.devapp.walkietalkiek.feature.ptt.model.PttScreenState

internal class TalkDurationChangedReducer :
    Reducer<PttAction.TalkDurationChanged, PttScreenState, PttAction, PttEvent> {

    override val actionClass = PttAction.TalkDurationChanged::class

    override suspend fun reduce(
        action: PttAction.TalkDurationChanged,
        getState: () -> PttScreenState
    ): Reducer.Result<PttScreenState, PttAction, PttEvent?> {
        return Reducer.Result(
            state = getState().copy(
                talkDurationSeconds = action.seconds.coerceAtLeast(5),
                remainingTalkSeconds = action.seconds.coerceAtLeast(5),
                remainingTalkMillis = action.seconds.coerceAtLeast(5) * 1000L
            ),
            event = null
        )
    }
}
