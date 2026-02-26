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
        val selfNodeToken = state.selfNodeId.takeIf { it.isNotBlank() }?.let { "node:$it" }
        val selfIpToken = state.myIP.takeIf { it.isNotBlank() && it != "-" && it != "--" }?.let { "node:$it" }
        val grantedToMe = owner != null && (
            owner == selfNodeToken ||
                owner == selfIpToken
            )
        if (state.isFloorRequestPending && grantedToMe) {
            return Reducer.Result(
                state = state.copy(
                    floorOwnerHostAddress = owner,
                    isFloorHeldByMe = true
                ),
                action = PttAction.StartRecordingGranted,
                event = null
            )
        }
        if (state.isFloorHeldByMe && owner == null) {
            return Reducer.Result(
                state = state.copy(
                    isFloorHeldByMe = false,
                    isFloorRequestPending = false,
                    floorOwnerHostAddress = null
                ),
                action = if (state.isRecording) PttAction.StopRecording else null,
                event = null
            )
        }
        if (state.isFloorHeldByMe && owner != null && owner != state.floorOwnerHostAddress) {
            return Reducer.Result(
                state = state.copy(
                    isFloorHeldByMe = false,
                    isFloorRequestPending = false,
                    floorOwnerHostAddress = owner
                ),
                action = if (state.isRecording) PttAction.StopRecording else null,
                event = null
            )
        }
        val nextState = if (state.isFloorHeldByMe) {
            state.copy(floorOwnerHostAddress = owner ?: state.floorOwnerHostAddress)
        } else {
            state.copy(
                floorOwnerHostAddress = owner,
                isFloorHeldByMe = false,
                isFloorRequestPending = if (owner == null) state.isFloorRequestPending else false
            )
        }
        return Reducer.Result(
            state = nextState,
            event = null
        )
    }
}
