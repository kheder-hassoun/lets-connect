package pro.devapp.walkietalkiek.feature.ptt.reducer

import pro.devapp.walkietalkiek.core.mvi.Reducer
import pro.devapp.walkietalkiek.feature.ptt.model.PttAction
import pro.devapp.walkietalkiek.feature.ptt.model.PttEvent
import pro.devapp.walkietalkiek.feature.ptt.model.PttScreenState

internal class ClusterStabilizationChangedReducer :
    Reducer<PttAction.ClusterStabilizationChanged, PttScreenState, PttAction, PttEvent> {

    override val actionClass = PttAction.ClusterStabilizationChanged::class

    override suspend fun reduce(
        action: PttAction.ClusterStabilizationChanged,
        getState: () -> PttScreenState
    ): Reducer.Result<PttScreenState, PttAction, PttEvent?> {
        return Reducer.Result(
            state = getState().copy(
                isClusterStabilizing = action.isVisible,
                clusterStabilizingTitle = action.title,
                clusterStabilizingDetail = action.detail
            ),
            event = null
        )
    }
}
