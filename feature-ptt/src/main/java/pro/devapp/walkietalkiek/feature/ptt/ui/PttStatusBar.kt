package pro.devapp.walkietalkiek.feature.ptt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import pro.devapp.walkietalkiek.feature.ptt.model.PttScreenState

@Composable
internal fun PttStatusBar(
    state: PttScreenState,
    canTalk: Boolean,
    modifier: Modifier = Modifier
) {
    val totalPeers = state.connectedDevices.size
    val connectedPeers = state.connectedDevices.count { it.isConnected }
    val isFloorBusyByRemote = state.floorOwnerHostAddress != null &&
        !state.isFloorHeldByMe &&
        !state.isRecording
    val modeColor = when {
        state.isRecording -> Color(0xFFFFA726)
        isFloorBusyByRemote -> Color(0xFFFF6B6B)
        canTalk -> Color(0xFF56E39F)
        else -> Color(0xFFFF6B6B)
    }
    val modeLabel = when {
        state.isRecording -> "Talking"
        isFloorBusyByRemote -> "Busy"
        canTalk -> "Enabled"
        else -> "Disabled"
    }
    val localColor = if (canTalk) Color(0xFF56E39F) else Color(0xFFFF6B6B)
    val localLabel = if (canTalk) "Online" else "Offline"
    val timerLabel = if (state.isRecording) {
        "Time Left ${state.remainingTalkSeconds}s"
    } else if (isFloorBusyByRemote) {
        "Channel Busy"
    } else {
        "Max Talk ${state.talkDurationSeconds}s"
    }
    val timerColor = when {
        state.isRecording -> Color(0xFFFFB347)
        isFloorBusyByRemote -> Color(0xFFFF6B6B)
        else -> Color(0xFF6FD3FF)
    }
    val peersColor = if (connectedPeers > 0) Color(0xFF44D39D) else Color(0xFF9AA4B2)
    val sortedPeers = remember(state.connectedDevices) {
        state.connectedDevices.sortedWith(
            compareByDescending<pro.devapp.walkietalkiek.serivce.network.data.model.ClientModel> { it.isConnected }
                .thenBy { it.hostAddress }
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            modeColor.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.26f),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusPill(
                    modifier = Modifier.weight(1f),
                    title = "LOCAL",
                    value = localLabel,
                    tint = localColor
                )
                StatusPill(
                    modifier = Modifier.weight(1f),
                    title = "PTT",
                    value = modeLabel,
                    tint = modeColor
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoPill(
                    modifier = Modifier.weight(1f),
                    label = "Peers: $connectedPeers/$totalPeers",
                    labelColor = peersColor,
                    containerColor = peersColor.copy(alpha = 0.14f)
                )
                InfoPill(
                    modifier = Modifier.weight(1f),
                    label = timerLabel,
                    labelColor = timerColor,
                    containerColor = timerColor.copy(alpha = 0.14f)
                )
            }

            InfoPill(
                modifier = Modifier.fillMaxWidth(),
                label = if (isFloorBusyByRemote) {
                    "Speaker: ${state.floorOwnerHostAddress}"
                } else {
                    "IP: ${state.myIP}"
                }
            )

            PeersChipsRow(
                peers = sortedPeers
            )
        }
    }
}

@Composable
private fun StatusPill(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    tint: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = tint.copy(alpha = 0.14f),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = tint.copy(alpha = 0.9f),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = tint,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun InfoPill(
    modifier: Modifier = Modifier,
    label: String,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = labelColor,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PeersChipsRow(
    peers: List<pro.devapp.walkietalkiek.serivce.network.data.model.ClientModel>
) {
    val scrollState = rememberScrollState()
    val previewPeers = peers.take(6)
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Peer Status",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (previewPeers.isEmpty()) {
            Text(
                text = "No peers",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            previewPeers.forEach { peer ->
                val chipColor = if (peer.isConnected) Color(0xFF3BD98A) else Color(0xFFFF8A80)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = chipColor.copy(alpha = 0.16f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (peer.isConnected) "●" else "○",
                            color = chipColor,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${peer.hostAddress}:${peer.port}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                            color = chipColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
