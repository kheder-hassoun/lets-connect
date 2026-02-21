package pro.devapp.walkietalkiek.feature.ptt.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
internal fun PTTContentPortrait(
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
    val contentPadding = (screenWidth * 0.035f).coerceIn(12.dp, 24.dp)
    val buttonSize = screenWidth.coerceAtMost(screenHeight * 0.48f).coerceIn(190.dp, 320.dp)
    val buttonAreaHeight = (screenHeight * 0.4f).coerceIn(240.dp, 390.dp)
    val peersPanelHeight = (screenHeight * 0.17f).coerceIn(96.dp, 148.dp)
    val waveHeight = (screenHeight * 0.075f).coerceIn(36.dp, 72.dp)

    Column(
        modifier = Modifier.fillMaxSize()
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
                .padding(horizontal = contentPadding, vertical = 8.dp)
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
        ConnectedPeersList(
            modifier = Modifier
                .height(peersPanelHeight)
                .padding(top = 4.dp),
            devices = state.connectedDevices
        )
        Spacer(
            modifier = Modifier.height(10.dp)
        )
    }
}
