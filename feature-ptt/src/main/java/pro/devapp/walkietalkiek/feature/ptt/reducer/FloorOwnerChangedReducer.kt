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
        val selfNodeId = state.selfNodeId.trim()
        val selfNodeToken = selfNodeId.takeIf { it.isNotBlank() }?.let { "node:$it" }
        val selfIpToken = state.myIP.takeIf { it.isNotBlank() && it != "-" && it != "--" }?.let { "node:$it" }
        val ownerNodeId = owner?.removePrefix("node:")?.trim().orEmpty()
        val grantedToMe = owner != null && (
            owner.equals(selfNodeToken, ignoreCase = true) ||
                owner.equals(selfIpToken, ignoreCase = true) ||
                (selfNodeId.isNotBlank() && ownerNodeId.equals(selfNodeId, ignoreCase = true))
            )
        if (state.isFloorRequestPending && grantedToMe) {
            return Reducer.Result(
                state = state.copy(
                    floorOwnerHostAddress = owner,
                    isFloorHeldByMe = true,
                    isFloorRequestPending = false,
                    isRemoteSpeaking = false
                ),
                action = PttAction.StartRecordingGranted,
                event = null
            )
        }
        if (state.isFloorRequestPending && owner != null && !grantedToMe) {
            return Reducer.Result(
                state = state.copy(
                    floorOwnerHostAddress = owner,
                    isFloorHeldByMe = false,
                    isFloorRequestPending = true
                ),
                event = null
            )
        }
        if (state.isFloorHeldByMe && owner == null) {
            return Reducer.Result(
                state = state.copy(
                    isFloorHeldByMe = false,
                    isFloorRequestPending = false,
                    floorOwnerHostAddress = null,
                    isRemoteSpeaking = false,
                    voiceData = null
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
                    floorOwnerHostAddress = owner,
                    isRemoteSpeaking = owner != null && !state.isRecording
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
                isFloorRequestPending = if (owner == null) state.isFloorRequestPending else false,
                isRemoteSpeaking = if (owner == null) false else !state.isRecording,
                voiceData = if (owner == null) null else state.voiceData
            )
        }
        return Reducer.Result(
            state = nextState,
            event = null
        )
    }
}
