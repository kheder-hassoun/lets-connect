package pro.devapp.walkietalkiek.feature.ptt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pro.devapp.walkietalkiek.feature.ptt.model.PttScreenState

@Composable
internal fun PttStatusBar(
    state: PttScreenState,
    canTalk: Boolean,
    modifier: Modifier = Modifier
) {
    val cfg = LocalConfiguration.current
    val screenWidth = cfg.screenWidthDp.dp
    val screenHeight = cfg.screenHeightDp.dp
    val scale = (screenWidth.coerceAtMost(screenHeight) / 400.dp).coerceIn(0.8f, 1.2f)

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
        state.isFloorRequestPending -> "Waiting"
        isFloorBusyByRemote -> "Busy"
        canTalk -> "Ready"
        else -> "Offline"
    }

    val roleColor = if (state.clusterRoleLabel == "Leader") Color(0xFFE53935) else Color(0xFF1E88E5)
    val floorStatus = when {
        state.isRecording || state.isFloorHeldByMe -> "Mine"
        state.isFloorRequestPending -> "Ask"
        isFloorBusyByRemote -> "Busy"
        else -> "Free"
    }

    val panelShape = RoundedCornerShape((16 * scale).dp)
    val cardShape = RoundedCornerShape((12 * scale).dp)
    val cardWidth = (screenWidth * 0.44f).coerceIn(150.dp, 210.dp)
    val cardHeight = (92 * scale).dp
    val gap = (6 * scale).dp
    val scrollState = rememberScrollState()

    LaunchedEffect(scrollState.maxValue) {
        if (scrollState.maxValue <= 0) return@LaunchedEffect
        while (true) {
            scrollState.animateScrollTo(
                value = scrollState.maxValue,
                animationSpec = tween(durationMillis = 7600, easing = LinearEasing)
            )
            delay(450L)
            scrollState.animateScrollTo(
                value = 0,
                animationSpec = tween(durationMillis = 7600, easing = LinearEasing)
            )
            delay(450L)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = (14 * scale).dp),
        shape = panelShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            modeColor.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                    shape = panelShape
                )
                .padding((10 * scale).dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight)
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                TinyStatusCard(
                    title = "PTT",
                    l1 = "Mode $modeLabel",
                    l2 = if (canTalk) "Local Online" else "Local Offline",
                    accent = modeColor,
                    shape = cardShape,
                    scale = scale,
                    modifier = Modifier.width(cardWidth)
                )

                TinyStatusCard(
                    title = "Cluster",
                    l1 = "Role ${state.clusterRoleLabel}",
                    l2 = "Peers $connectedPeers",
                    accent = roleColor,
                    shape = cardShape,
                    scale = scale,
                    modifier = Modifier.width(cardWidth)
                )

                TinyStatusCard(
                    title = "Floor",
                    l1 = "State $floorStatus",
                    l2 = if (isFloorBusyByRemote) "Remote speaking" else "Open",
                    accent = Color(0xFF6FD3FF),
                    shape = cardShape,
                    scale = scale,
                    modifier = Modifier.width(cardWidth)
                )

                Spacer(modifier = Modifier.width((24 * scale).dp))
            }

            Spacer(modifier = Modifier.height((6 * scale).dp))
            StatusTypewriter(
                scale = scale,
                color = modeColor
            )
        }
    }
}

@Composable
private fun TinyStatusCard(
    title: String,
    l1: String,
    l2: String,
    accent: Color,
    shape: RoundedCornerShape,
    scale: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = accent.copy(alpha = 0.35f),
                    shape = shape
                )
                .padding(horizontal = (10 * scale).dp, vertical = (8 * scale).dp),
            verticalArrangement = Arrangement.spacedBy((2 * scale).dp)
        ) {
            Text(
                text = title,
                fontSize = (11 * scale).sp,
                color = accent,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = l1,
                fontSize = (12 * scale).sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = l2,
                fontSize = (11 * scale).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatusTypewriter(
    scale: Float,
    color: Color
) {
    val lines = remember {
        listOf(
            "Let's Connect is your choice",
            "Voice calls and chat support",
            "No internet needed"
        )
    }
    var lineIndex by remember { mutableStateOf(0) }
    var visibleText by remember { mutableStateOf("") }

    val cursorTransition = rememberInfiniteTransition(label = "ptt-status-cursor")
    val cursorAlpha by cursorTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(450),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ptt-status-cursor-alpha"
    )

    LaunchedEffect(lines) {
        while (true) {
            val line = lines[lineIndex]
            for (i in 1..line.length) {
                visibleText = line.substring(0, i)
                delay(48)
            }
            delay(900)
            for (i in line.length downTo 0) {
                visibleText = line.substring(0, i)
                delay(30)
            }
            lineIndex = (lineIndex + 1) % lines.size
            delay(200)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(
            text = visibleText,
            fontSize = (11 * scale).sp,
            color = color,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "|",
            fontSize = (11 * scale).sp,
            color = color.copy(alpha = cursorAlpha),
            fontWeight = FontWeight.Bold
        )
    }
}
