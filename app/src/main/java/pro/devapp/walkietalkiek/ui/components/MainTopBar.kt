package pro.devapp.walkietalkiek.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
                AnimatedBrandGifIcon(
                    size = topBarGifSize
                )
            }
        }
    }
}
