package pro.devapp.walkietalkiek.feature.ptt.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.adaptive.currentWindowSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.toSize
import org.koin.compose.koinInject
import pro.devapp.walkietalkiek.feature.ptt.PttViewModel
import pro.devapp.walkietalkiek.feature.ptt.model.PttAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PTTContent(
    modifier: Modifier = Modifier
) {
    val viewModel: PttViewModel = koinInject()
    val state = viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onPttAction(PttAction.InitScreen)
        viewModel.startCollectingConnectedDevices()
    }

    val windowSize = with(LocalDensity.current) {
        currentWindowSize().toSize().toDpSize()
    }
    Box(modifier = modifier.fillMaxSize()) {
        if (windowSize.width > windowSize.height) {
            // Landscape mode
            PTTContentLandscape(
                state = state.value,
                onAction = viewModel::onPttAction
            )
        } else {
            // Portrait mode
            PTTContentPortrait(
                state = state.value,
                onAction = viewModel::onPttAction
            )
        }
        if (state.value.isClusterStabilizing) {
            ClusterStabilizingOverlay(
                title = state.value.clusterStabilizingTitle,
                detail = state.value.clusterStabilizingDetail
            )
        }
    }
}
