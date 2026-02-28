package pro.devapp.walkietalkiek.feature.ptt.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.Arrangement
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
    val isFloorOwnedByRemote = state.floorOwnerHostAddress != null &&
        !state.isFloorHeldByMe &&
        !state.isRecording
    val isFloorBusyByRemote = isFloorOwnedByRemote || (state.isRemoteSpeaking && !state.isRecording)
    val canPressPtt = ((canTalk && !isFloorBusyByRemote) || state.isRecording)
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val scale = (screenWidth.coerceAtMost(screenHeight) / 400.dp).coerceIn(0.8f, 1.2f)
    val contentPadding = (screenWidth * 0.035f).coerceIn(10.dp, 24.dp)
    val buttonSize = screenWidth.coerceAtMost(screenHeight * 0.48f).coerceIn(190.dp, 320.dp)
    val buttonAreaHeight = (screenHeight * 0.45f).coerceIn(260.dp, 430.dp)
    val peersPanelHeight = (screenHeight * 0.14f).coerceIn(86.dp, 150.dp)
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
        ConnectedPeersList(
            modifier = Modifier
                .height(peersPanelHeight)
                .padding(top = (4 * scale).dp),
            devices = state.connectedDevices
        )

        Box(
            modifier = Modifier
                .padding(horizontal = contentPadding, vertical = (8 * scale).dp)
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .sizeIn(maxHeight = buttonAreaHeight)
                    .height(buttonAreaHeight)
                    .fillMaxWidth(),
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
                Spacer(modifier = Modifier.height((6 * scale).dp))
                WaveCanvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(waveHeight),
                    data = state.voiceData,
                )
            }
        }
        Spacer(
            modifier = Modifier.height((6 * scale).dp)
        )
    }
}
