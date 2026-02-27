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
    val isFloorOwnedByRemote = state.floorOwnerHostAddress != null &&
        !state.isFloorHeldByMe &&
        !state.isRecording
    val isFloorBusyByRemote = isFloorOwnedByRemote || (state.isRemoteSpeaking && !state.isRecording)
    val canPressPtt = (canTalk && !isFloorBusyByRemote) || state.isRecording
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val scale = (screenWidth.coerceAtMost(screenHeight) / 400.dp).coerceIn(0.8f, 1.2f)
    val contentPadding = (screenWidth * 0.02f).coerceIn(8.dp, 24.dp)
    val buttonSize = (screenHeight * 0.52f).coerceIn(190.dp, 300.dp)
    val waveHeight = (screenHeight * 0.055f).coerceIn(20.dp, 48.dp)
    val buttonAreaHeight = (screenHeight * 0.44f).coerceIn(220.dp, 320.dp)
    val peersPanelHeight = (screenHeight * 0.16f).coerceIn(84.dp, 140.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy((6 * scale).dp)
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
                .padding(horizontal = (6 * scale).dp, vertical = (4 * scale).dp)
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
                    .padding(horizontal = (14 * scale).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PTTButton(
                    modifier = Modifier.padding((6 * scale).dp),
                    buttonSize = buttonSize,
                    isOnline = canTalk,
                    isEnabled = canPressPtt,
                    isLockedByRemote = isFloorBusyByRemote,
                    isRemoteSpeaking = state.isRemoteSpeaking,
                    isRecording = state.isRecording,
                    remainingMillis = state.remainingTalkMillis,
                    totalSeconds = state.talkDurationSeconds,
                    onPress = { onAction(PttAction.StartRecording) },
                    onRelease = { onAction(PttAction.StopRecording) }
                )
                Spacer(
                    modifier = Modifier.height((6 * scale).dp)
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
