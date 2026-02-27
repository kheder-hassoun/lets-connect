package pro.devapp.walkietalkiek.feature.ptt.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import pro.devapp.walkietalkiek.feature.ptt.model.PttAction
import pro.devapp.walkietalkiek.feature.ptt.model.PttScreenState

@Composable
internal fun PTTContentLandscape(
    state: PttScreenState,
    onAction: (PttAction) -> Unit,
) {
    val hasValidIp = state.myIP != "-" && state.myIP != "--" && state.myIP.isNotBlank()
    val hasConnectedPeers = state.connectedDevices.any { it.isConnected }
    val canTalk = hasValidIp || hasConnectedPeers
    val isFloorBusyByRemote = state.floorOwnerHostAddress != null &&
        !state.isFloorHeldByMe &&
        !state.isRecording
    val canPressPtt = (canTalk && !isFloorBusyByRemote) || state.isRecording
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val contentPadding = (screenWidth * 0.02f).coerceIn(10.dp, 24.dp)
    val buttonSize = (screenHeight * 0.52f).coerceIn(190.dp, 300.dp)
    val waveHeight = (screenHeight * 0.055f).coerceIn(20.dp, 48.dp)
    val buttonAreaHeight = (screenHeight * 0.44f).coerceIn(220.dp, 320.dp)
    val peersPanelHeight = (screenHeight * 0.2f).coerceIn(120.dp, 180.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MyDeviceInfo(
            isOnline = canTalk,
            addressIp = state.myIP
        )
        PttStatusBar(
            state = state,
            canTalk = canTalk
        )
        ConnectedPeersList(
            modifier = Modifier.height(peersPanelHeight),
            devices = state.connectedDevices
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .sizeIn(maxHeight = buttonAreaHeight)
                    .height(buttonAreaHeight)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PTTButton(
                    modifier = Modifier.padding(8.dp),
                    buttonSize = buttonSize,
                    isOnline = canTalk,
                    isEnabled = canPressPtt,
                    isRecording = state.isRecording,
                    remainingMillis = state.remainingTalkMillis,
                    totalSeconds = state.talkDurationSeconds,
                    onPress = { onAction(PttAction.StartRecording) },
                    onRelease = { onAction(PttAction.StopRecording) }
                )
                Spacer(
                    modifier = Modifier.height(8.dp)
                )
                WaveCanvas(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(waveHeight),
                    data = state.voiceData,
                )
            }
        }
    }
}
