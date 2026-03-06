package pro.devapp.walkietalkiek.feature.ptt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pro.devapp.walkietalkiek.feature.ptt.R
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
    val isFloorOwnedByRemote = state.floorOwnerHostAddress != null &&
        !state.isFloorHeldByMe &&
        !state.isRecording
    val isFloorBusyByRemote = isFloorOwnedByRemote || (state.isRemoteSpeaking && !state.isRecording)

    val modeColor = when {
        state.isRecording -> Color(0xFFFFA726)
        state.isFloorRequestPending -> Color(0xFFFFB347)
        isFloorBusyByRemote -> Color(0xFFFF6B6B)
        canTalk -> Color(0xFF56E39F)
        else -> Color(0xFFFF6B6B)
    }
    val modeLabel = when {
        state.isRecording -> stringResource(R.string.ptt_mode_talking)
        state.isFloorRequestPending -> stringResource(R.string.ptt_mode_waiting)
        isFloorBusyByRemote -> stringResource(R.string.ptt_mode_busy)
        canTalk -> stringResource(R.string.ptt_mode_ready)
        else -> stringResource(R.string.ptt_mode_offline)
    }
    val roleLabelLocalized = localizeRoleLabel(state.clusterRoleLabel)
    val isLeaderRole = roleLabelLocalized == stringResource(R.string.ptt_role_admin)
    val roleColor = if (isLeaderRole) Color(0xFFE53935) else Color(0xFF1E88E5)
    val floorStatus = when {
        state.isRecording || state.isFloorHeldByMe -> stringResource(R.string.ptt_floor_mine)
        state.isFloorRequestPending -> stringResource(R.string.ptt_floor_ask)
        isFloorBusyByRemote -> stringResource(R.string.ptt_floor_busy)
        else -> stringResource(R.string.ptt_floor_free)
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
            Text(
                text = stringResource(R.string.ptt_status_panel_title),
                fontSize = (12 * scale).sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height((6 * scale).dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight)
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                TinyStatusCard(
                    title = stringResource(R.string.ptt_card_ptt_title),
                    l1 = stringResource(R.string.ptt_card_mode, modeLabel),
                    l2 = if (canTalk) {
                        stringResource(R.string.ptt_card_local_online)
                    } else {
                        stringResource(R.string.ptt_card_local_offline)
                    },
                    accent = modeColor,
                    shape = cardShape,
                    scale = scale,
                    modifier = Modifier.width(cardWidth)
                )

                TinyStatusCard(
                    title = stringResource(R.string.ptt_card_cluster_title),
                    l1 = stringResource(R.string.ptt_card_role, roleLabelLocalized),
                    l2 = stringResource(R.string.ptt_card_peers, connectedPeers),
                    accent = roleColor,
                    shape = cardShape,
                    scale = scale,
                    modifier = Modifier.width(cardWidth)
                )

                TinyStatusCard(
                    title = stringResource(R.string.ptt_card_floor_title),
                    l1 = stringResource(R.string.ptt_card_state, floorStatus),
                    l2 = if (isFloorBusyByRemote) {
                        stringResource(R.string.ptt_card_remote_speaking)
                    } else {
                        stringResource(R.string.ptt_card_open)
                    },
                    accent = Color(0xFF6FD3FF),
                    shape = cardShape,
                    scale = scale,
                    modifier = Modifier.width(cardWidth)
                )

                Spacer(modifier = Modifier.width((24 * scale).dp))
            }

            Spacer(modifier = Modifier.height((8 * scale).dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = (24 * scale).dp.coerceAtLeast(22.dp)),
                contentAlignment = androidx.compose.ui.Alignment.CenterStart
            ) {
                val speakNowText = if (connectedPeers == 0) {
                    stringResource(R.string.ptt_speak_now_no_peers)
                } else {
                    stringResource(R.string.ptt_speak_now)
                }
                Text(
                    text = speakNowText,
                    fontSize = (12 * scale).sp,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    modifier = Modifier.alpha(if (state.isRecording) 1f else 0f)
                )
            }
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
private fun localizeRoleLabel(rawRoleLabel: String): String {
    return when (rawRoleLabel.lowercase()) {
        "leader", "admin", "أدمن" -> stringResource(R.string.ptt_role_admin)
        "peer", "user", "مستخدم" -> stringResource(R.string.ptt_role_user)
        else -> rawRoleLabel
    }
}
