package pro.devapp.walkietalkiek.feature.ptt.reducer

import pro.devapp.walkietalkiek.core.mvi.Reducer
import pro.devapp.walkietalkiek.feature.ptt.model.PttAction
import pro.devapp.walkietalkiek.feature.ptt.model.PttEvent
import pro.devapp.walkietalkiek.feature.ptt.model.PttScreenState
import kotlin.math.ceil

internal class TalkTimerTickReducer :
    Reducer<PttAction.TalkTimerTick, PttScreenState, PttAction, PttEvent> {

    override val actionClass = PttAction.TalkTimerTick::class

    override suspend fun reduce(
        action: PttAction.TalkTimerTick,
        getState: () -> PttScreenState
    ): Reducer.Result<PttScreenState, PttAction, PttEvent?> {
        val millis = action.remainingMillis.coerceAtLeast(0L)
        return Reducer.Result(
            state = getState().copy(
                remainingTalkMillis = millis,
                remainingTalkSeconds = ceil(millis / 1000.0).toInt()
            ),
            event = null
        )
    }
}
