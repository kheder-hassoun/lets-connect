package pro.devapp.walkietalkiek.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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

@Composable
fun MainTopBar(
    modifier: Modifier = Modifier,
    state: MainScreenState,
    isPttEnabled: Boolean
) {
    val accent = MaterialTheme.colorScheme.primary
    val clusterMembershipRepository = koinInject<ClusterMembershipRepository>()
    val clusterStatus by clusterMembershipRepository.status.collectAsState()
    val roleLabel = if (clusterStatus.role == ClusterRole.LEADER) "Leader" else "Peer"
    val currentTab = state.mainTabs.firstOrNull { it.screen == state.currentTab }
    val currentTabTitle = currentTab?.title ?: state.currentTab.name

    val cfg = androidx.compose.ui.platform.LocalConfiguration.current
    val scale = (cfg.screenWidthDp.dp.coerceAtMost(cfg.screenHeightDp.dp) / 400.dp).coerceIn(0.84f, 1.18f)

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
                    color = if (roleLabel == "Leader") Color(0xFFE53935) else Color(0xFF1E88E5),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((8 * scale).dp)
            ) {
                SignalScanner(
                    isConnected = isPttEnabled,
                    scale = scale
                )
                Icon(
                    painter = painterResource(id = currentTab?.icon ?: R.drawable.ptt),
                    contentDescription = currentTabTitle,
                    tint = accent,
                    modifier = Modifier.size((18 * scale).dp)
                )
            }
        }
    }
}

@Composable
private fun SignalScanner(
    isConnected: Boolean,
    scale: Float
) {
    val transition = rememberInfiniteTransition(label = "scanner")
    val pulse1 by transition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse-1"
    )
    val pulse2 by transition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse-2"
    )

    val color = if (isConnected) Color(0xFF56E39F) else Color(0xFFFFB347)

    Box(
        modifier = Modifier.size((24 * scale).dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size((22 * scale).dp)
                .scale(pulse2)
                .alpha(0.15f)
                .border(1.dp, color, CircleShape)
        )
        Box(
            modifier = Modifier
                .size((18 * scale).dp)
                .scale(pulse1)
                .alpha(0.22f)
                .border(1.dp, color, CircleShape)
        )
        Icon(
            painter = painterResource(id = R.drawable.connection_on),
            contentDescription = if (isConnected) "Connected" else "Scanning",
            tint = color,
            modifier = Modifier.size((14 * scale).dp)
        )
    }
}
