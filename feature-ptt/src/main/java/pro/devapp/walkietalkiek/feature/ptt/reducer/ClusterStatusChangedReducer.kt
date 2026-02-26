package pro.devapp.walkietalkiek.feature.ptt.reducer

import pro.devapp.walkietalkiek.core.mvi.Reducer
import pro.devapp.walkietalkiek.feature.ptt.model.PttAction
import pro.devapp.walkietalkiek.feature.ptt.model.PttEvent
import pro.devapp.walkietalkiek.feature.ptt.model.PttScreenState

internal class ClusterStatusChangedReducer :
    Reducer<PttAction.ClusterStatusChanged, PttScreenState, PttAction, PttEvent> {

    override val actionClass = PttAction.ClusterStatusChanged::class

    override suspend fun reduce(
        action: PttAction.ClusterStatusChanged,
        getState: () -> PttScreenState
    ): Reducer.Result<PttScreenState, PttAction, PttEvent?> {
        val state = getState()
        return Reducer.Result(
            state = state.copy(
                selfNodeId = if (action.selfNodeId.isNotBlank()) {
                    action.selfNodeId
                } else {
                    state.selfNodeId
                },
                clusterRoleLabel = action.roleLabel,
                leaderNodeLabel = action.leaderNodeLabel,
                clusterMembersCount = action.membersCount
            ),
            event = null
        )
    }
}
