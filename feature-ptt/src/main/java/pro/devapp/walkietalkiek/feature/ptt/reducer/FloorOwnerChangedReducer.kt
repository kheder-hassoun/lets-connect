package pro.devapp.walkietalkiek.feature.ptt.reducer

import pro.devapp.walkietalkiek.core.mvi.Reducer
import pro.devapp.walkietalkiek.feature.ptt.model.PttAction
import pro.devapp.walkietalkiek.feature.ptt.model.PttEvent
import pro.devapp.walkietalkiek.feature.ptt.model.PttScreenState

internal class FloorOwnerChangedReducer :
    Reducer<PttAction.FloorOwnerChanged, PttScreenState, PttAction, PttEvent> {

    override val actionClass = PttAction.FloorOwnerChanged::class

    override suspend fun reduce(
        action: PttAction.FloorOwnerChanged,
        getState: () -> PttScreenState
    ): Reducer.Result<PttScreenState, PttAction, PttEvent?> {
        val state = getState()
        val owner = action.ownerHostAddress
        val nextState = when {
            state.isFloorHeldByMe && owner != null -> state
            state.isFloorHeldByMe && owner == null -> state.copy(floorOwnerHostAddress = null)
            else -> state.copy(
                floorOwnerHostAddress = owner,
                isFloorHeldByMe = false
            )
        }
        return Reducer.Result(
            state = nextState,
            event = null
        )
    }
}
