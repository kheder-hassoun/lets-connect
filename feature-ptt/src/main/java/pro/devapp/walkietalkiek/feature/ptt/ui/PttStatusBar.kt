package pro.devapp.walkietalkiek.feature.ptt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import kotlinx.coroutines.delay
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
        state.isFloorRequestPending -> Color(0xFFFFB347)
        isFloorBusyByRemote -> Color(0xFFFF6B6B)
        canTalk -> Color(0xFF56E39F)
        else -> Color(0xFFFF6B6B)
    }
    val modeLabel = when {
        state.isRecording -> "Talking"
        state.isFloorRequestPending -> "Requesting"
        isFloorBusyByRemote -> "Busy"
        canTalk -> "Enabled"
        else -> "Disabled"
    }
    val localColor = if (canTalk) Color(0xFF56E39F) else Color(0xFFFF6B6B)
    val localLabel = if (canTalk) "Online" else "Offline"
    val roleColor = if (state.clusterRoleLabel == "Leader") Color(0xFFE53935) else Color(0xFF1E88E5)
    val timerLabel = if (state.isRecording) "Time Left ${state.remainingTalkSeconds}s" else "Max Talk ${state.talkDurationSeconds}s"
    val timerColor = when {
        state.isRecording -> Color(0xFFFFB347)
        isFloorBusyByRemote -> Color(0xFFFF6B6B)
        else -> Color(0xFF6FD3FF)
    }
    val floorStatus = when {
        state.isRecording || state.isFloorHeldByMe -> "Mine"
        state.isFloorRequestPending -> "Requesting"
        isFloorBusyByRemote -> "Busy"
        else -> "Free"
    }
    val floorColor = when (floorStatus) {
        "Mine" -> Color(0xFF56E39F)
        "Requesting" -> Color(0xFFFFB347)
        "Busy" -> Color(0xFFFF6B6B)
        else -> Color(0xFF6FD3FF)
    }
    val peersColor = if (connectedPeers > 0) Color(0xFFFF5CA8) else Color(0xFFB39BB2)
    val ownerLabel = state.floorOwnerHostAddress ?: "--"
    val liveLine = if (isFloorBusyByRemote) "Speaker: $ownerLabel" else "IP: ${state.myIP}"
    val scrollState = rememberScrollState()
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val cardWidth = (screenWidth * 0.64f).coerceIn(230.dp, 360.dp)

    LaunchedEffect(scrollState.maxValue) {
        if (scrollState.maxValue <= 0) return@LaunchedEffect
        while (true) {
            delay(1000L)
            scrollState.animateScrollTo(
                value = scrollState.maxValue,
                animationSpec = tween(durationMillis = 4200, easing = LinearEasing)
            )
            delay(600L)
            scrollState.animateScrollTo(
                value = 0,
                animationSpec = tween(durationMillis = 4200, easing = LinearEasing)
            )
            delay(600L)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
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
                .padding(12.dp)
        ) {
            Text(
                text = "Status Panel",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(138.dp)
                    .horizontalScroll(scrollState)
            ) {
                StatusCard(
                    title = "PTT",
                    modifier = Modifier.width(cardWidth),
                    accent = modeColor
                ) {
                    StatusMetric(title = "LOCAL", value = localLabel, tint = localColor)
                    StatusMetric(title = "MODE", value = modeLabel, tint = modeColor)
                    StatusMetric(title = "FLOOR", value = floorStatus, tint = floorColor)
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusCard(
                    title = "CLUSTER",
                    modifier = Modifier.width(cardWidth),
                    accent = roleColor
                ) {
                    StatusMetric(title = "ROLE", value = state.clusterRoleLabel, tint = roleColor)
                    StatusMetric(title = "MEMBERS", value = state.clusterMembersCount.toString(), tint = MaterialTheme.colorScheme.primary)
                    StatusMetric(title = "LEADER", value = state.leaderNodeLabel, tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusCard(
                    title = "LIVE",
                    modifier = Modifier.width(cardWidth),
                    accent = peersColor
                ) {
                    StatusMetric(title = "PEERS", value = "$connectedPeers/$totalPeers", tint = peersColor)
                    StatusMetric(title = "TIMER", value = timerLabel, tint = timerColor)
                    StatusMetric(title = "NOW", value = liveLine, tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusCard(
                    title = "CONTROL",
                    modifier = Modifier.width(cardWidth),
                    accent = MaterialTheme.colorScheme.primary
                ) {
                    StatusMetric(title = "PLANE", value = state.controlPlaneLabel, tint = MaterialTheme.colorScheme.primary)
                    StatusMetric(title = "DETAIL", value = state.controlPlaneDetail, tint = MaterialTheme.colorScheme.onSurface)
                    StatusMetric(title = "NODE", value = state.selfNodeId.ifBlank { "--" }, tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    modifier: Modifier = Modifier,
    title: String,
    accent: Color,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = accent.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                content()
            }
        }
    }
}

@Composable
private fun StatusMetric(
    title: String,
    value: String,
    tint: Color
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
    )
    Text(
        text = value,
        style = MaterialTheme.typography.titleSmall,
        color = tint,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(bottom = 5.dp)
    )
}
