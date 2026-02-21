package pro.devapp.walkietalkiek.feature.ptt.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
    val contentPadding = (screenWidth * 0.02f).coerceIn(10.dp, 28.dp)
    val buttonSize = (screenHeight * 0.56f).coerceIn(190.dp, 290.dp)
    val waveHeight = (screenHeight * 0.055f).coerceIn(20.dp, 48.dp)
    val buttonAreaHeight = (screenHeight * 0.68f).coerceIn(220.dp, 390.dp)
    val peersPanelHeight = (screenHeight * 0.58f).coerceIn(170.dp, 360.dp)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(0.4f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ConnectedPeersList(
                modifier = Modifier.height(peersPanelHeight),
                devices = state.connectedDevices
            )
        }
        Column(
            modifier = Modifier.weight(0.6f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MyDeviceInfo(
                isOnline = canTalk,
                addressIp = state.myIP
            )
            PttStatusBar(
                state = state,
                canTalk = canTalk
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .height(buttonAreaHeight)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                PTTButton(
                    modifier = Modifier.padding(8.dp),
                    buttonSize = buttonSize,
                    isOnline = canTalk,
                    isEnabled = canPressPtt,
                    isLockedByRemote = isFloorBusyByRemote,
                    isRecording = state.isRecording,
                    remainingSeconds = state.remainingTalkSeconds,
                    remainingMillis = state.remainingTalkMillis,
                    totalSeconds = state.talkDurationSeconds,
                    onPress = { onAction(PttAction.StartRecording) },
                    onRelease = { onAction(PttAction.StopRecording) }
                )
            }
            WaveCanvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(waveHeight),
                data = state.voiceData,
            )
            Spacer(
                modifier = Modifier.height(8.dp)
            )
        }
    }
}
