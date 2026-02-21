package pro.devapp.walkietalkiek.feature.ptt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
                    label = "Peers: $connectedPeers/$totalPeers"
                )
                InfoPill(
                    modifier = Modifier.weight(1f),
                    label = timerLabel
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
    label: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
