package pro.devapp.walkietalkiek.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.koinInject
import pro.devapp.walkietalkiek.R
import pro.devapp.walkietalkiek.model.MainScreenState
import pro.devapp.walkietalkiek.serivce.network.data.ClusterMembershipRepository
import pro.devapp.walkietalkiek.serivce.network.data.ClusterRole
import pro.devapp.walkietalkiek.serivce.network.data.PttFloorRepository

@Composable
fun MainTopBar(
    modifier: Modifier = Modifier,
    state: MainScreenState
) {
    val accent = MaterialTheme.colorScheme.primary
    val clusterMembershipRepository = koinInject<ClusterMembershipRepository>()
    val pttFloorRepository = koinInject<PttFloorRepository>()
    val clusterStatus by clusterMembershipRepository.status.collectAsState()
    val floorOwner by pttFloorRepository.currentFloorOwnerHost.collectAsState()
    val isSomeoneTalking = floorOwner != null
    val isLeader = clusterStatus.role == ClusterRole.LEADER
    val roleLabel = if (isLeader) {
        stringResource(R.string.role_admin)
    } else {
        stringResource(R.string.role_user)
    }
    val currentTab = state.mainTabs.firstOrNull { it.screen == state.currentTab }
    val currentTabTitle = currentTab?.let { stringResource(it.titleRes) } ?: state.currentTab.name

    val cfg = androidx.compose.ui.platform.LocalConfiguration.current
    val minScreen = cfg.screenWidthDp.dp.coerceAtMost(cfg.screenHeightDp.dp)
    val scale = (minScreen / 400.dp).coerceIn(0.84f, 1.18f)
    val topBarGifSize = (minScreen * 0.12f).coerceIn(42.dp, 64.dp)

    Surface(
        modifier = modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = (10 * scale).dp, vertical = (6 * scale).dp),
        shape = RoundedCornerShape((22 * scale).dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 6.dp,
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.26f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(horizontal = (14 * scale).dp, vertical = (10 * scale).dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    fontSize = (16 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentTabTitle,
                    fontSize = (12 * scale).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = roleLabel,
                    fontSize = (11 * scale).sp,
                    color = if (isLeader) Color(0xFFE53935) else Color(0xFF1E88E5),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((8 * scale).dp)
            ) {
                WalkieConnectionGlyph(
                    iconSize = topBarGifSize,
                    isSomeoneTalking = isSomeoneTalking
                )
            }
        }
    }
}

@Composable
private fun WalkieConnectionGlyph(
    iconSize: androidx.compose.ui.unit.Dp,
    isSomeoneTalking: Boolean
) {
    val transition = rememberInfiniteTransition(label = "walkie-link")
    val beamShift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "walkie-link-shift"
    )
    val beamAlpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "walkie-link-alpha"
    )
    val lineColor = if (isSomeoneTalking) Color(0xFF42A5F5) else Color(0xFFFFB347)
    val gap = (iconSize * 0.26f).coerceIn(8.dp, 16.dp)
    val beamWidth = (iconSize * 1.14f).coerceIn(48.dp, 84.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(gap)
    ) {
        AnimatedBrandGifIcon(size = iconSize)

        Box(
            modifier = Modifier
                .width(beamWidth)
                .height((iconSize * 0.24f).coerceIn(9.dp, 16.dp)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            ) {
                val strokePx = 3.dp.toPx()
                val glowStrokePx = 8.dp.toPx()
                val baseLine = Brush.horizontalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.18f),
                        lineColor.copy(alpha = 0.35f),
                        lineColor.copy(alpha = 0.18f)
                    )
                )
                drawLine(
                    brush = baseLine,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )

                val forwardHead = size.width * beamShift
                val backwardHead = size.width * (1f - beamShift)
                val segmentSize = size.width * 0.34f
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0f),
                            lineColor.copy(alpha = 0.9f * beamAlpha),
                            lineColor.copy(alpha = 0f)
                        ),
                        startX = (forwardHead - segmentSize).coerceAtLeast(0f),
                        endX = (forwardHead + segmentSize).coerceAtMost(size.width)
                    ),
                    start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
                    strokeWidth = glowStrokePx,
                    cap = StrokeCap.Round
                )
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0f),
                            lineColor.copy(alpha = 0.72f * beamAlpha),
                            lineColor.copy(alpha = 0f)
                        ),
                        startX = (backwardHead - segmentSize).coerceAtLeast(0f),
                        endX = (backwardHead + segmentSize).coerceAtMost(size.width)
                    ),
                    start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
                    strokeWidth = glowStrokePx * 0.88f,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = lineColor.copy(alpha = beamAlpha),
                    radius = 2.4.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(
                        x = forwardHead,
                        y = size.height / 2f
                    )
                )
                drawCircle(
                    color = lineColor.copy(alpha = beamAlpha * 0.8f),
                    radius = 2.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(
                        x = backwardHead,
                        y = size.height / 2f
                    )
                )
            }
        }

        AnimatedBrandGifIcon(size = iconSize)
    }
}
