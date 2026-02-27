package pro.devapp.walkietalkiek.ui

import android.content.Intent
import androidx.activity.compose.LocalActivity
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import pro.devapp.walkietalkiek.R
import pro.devapp.walkietalkiek.core.settings.AppSettingsRepository
import pro.devapp.walkietalkiek.serivce.network.data.ClusterMembershipRepository
import pro.devapp.walkietalkiek.serivce.network.data.ConnectedDevicesRepository
import pro.devapp.walkietalkiek.serivce.network.data.DeviceInfoRepository
import pro.devapp.walkietalkiek.serivce.network.data.TextMessagesRepository
import pro.devapp.walkietalkiek.service.WalkieService
import kotlin.system.exitProcess

@Composable
internal fun OffContent() {
    val activity = LocalActivity.current
    val appSettingsRepository = koinInject<AppSettingsRepository>()
    val connectedDevicesRepository = koinInject<ConnectedDevicesRepository>()
    val textMessagesRepository = koinInject<TextMessagesRepository>()
    val clusterMembershipRepository = koinInject<ClusterMembershipRepository>()
    val deviceInfoRepository = koinInject<DeviceInfoRepository>()

    val clusterStatus by clusterMembershipRepository.status.collectAsState()
    val clients by connectedDevicesRepository.clientsFlow.collectAsState(initial = emptyList())

    val roleLabel = if (clusterStatus.role == pro.devapp.walkietalkiek.serivce.network.data.ClusterRole.LEADER) {
        "Leader"
    } else {
        "Peer"
    }
    val ip = deviceInfoRepository.getCurrentIp().orEmpty().ifBlank { "--" }
    val localPort = deviceInfoRepository.getCurrentDeviceInfo().port
    val peerPorts = clients.asSequence().filter { it.isConnected }.map { it.port }.distinct().sorted().take(2).toList()
    val portsLabel = if (peerPorts.isEmpty()) {
        "L:$localPort"
    } else {
        "L:$localPort  P:${peerPorts.joinToString("/")}"
    }

    val cfg = androidx.compose.ui.platform.LocalConfiguration.current
    val base = cfg.screenWidthDp.dp.coerceAtMost(cfg.screenHeightDp.dp)
    val scale = (base / 400.dp).coerceIn(0.84f, 1.18f)

    var statusText by remember { mutableStateOf("Ready") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding((14 * scale).dp),
        verticalArrangement = Arrangement.spacedBy((10 * scale).dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape((18 * scale).dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
                        shape = RoundedCornerShape((18 * scale).dp)
                    )
                    .padding((12 * scale).dp),
                verticalArrangement = Arrangement.spacedBy((8 * scale).dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Status",
                        fontSize = (18 * scale).sp,
                        fontWeight = FontWeight.Bold
                    )
                    androidx.compose.material3.Icon(
                        painter = painterResource(id = R.drawable.connection_on),
                        contentDescription = "status",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size((20 * scale).dp)
                    )
                }

                StatusTicker(scale = scale)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy((8 * scale).dp)
                ) {
                    MiniInfoCard(
                        title = "Role",
                        value = roleLabel,
                        iconRes = R.drawable.connection_on,
                        modifier = Modifier.weight(1f),
                        scale = scale
                    )
                    MiniInfoCard(
                        title = "IP",
                        value = ip,
                        iconRes = R.drawable.select_to_speak,
                        modifier = Modifier.weight(1f),
                        scale = scale
                    )
                }

                MiniInfoCard(
                    title = "Ports",
                    value = portsLabel,
                    iconRes = R.drawable.notifications_active,
                    modifier = Modifier.fillMaxWidth(),
                    scale = scale
                )

                Text(
                    text = statusText,
                    fontSize = (11 * scale).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                connectedDevicesRepository.clearAll()
                textMessagesRepository.clearAll()
                appSettingsRepository.resetToDefaults()
                statusText = "Settings reset"
            },
            shape = RoundedCornerShape((12 * scale).dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
            )
        ) {
            androidx.compose.material3.Icon(
                painter = painterResource(id = R.drawable.settings),
                contentDescription = "reset",
                modifier = Modifier.size((16 * scale).dp)
            )
            Spacer(modifier = Modifier.size((8 * scale).dp))
            Text("Reset Settings", fontSize = (13 * scale).sp, fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val serviceIntent = Intent(activity, WalkieService::class.java)
                activity?.stopService(serviceIntent)
                activity?.finishAndRemoveTask()
                exitProcess(0)
            },
            shape = RoundedCornerShape((12 * scale).dp)
        ) {
            androidx.compose.material3.Icon(
                painter = painterResource(id = R.drawable.power_off),
                contentDescription = "force close",
                modifier = Modifier.size((16 * scale).dp),
                tint = Color(0xFFFF6B6B)
            )
            Spacer(modifier = Modifier.size((8 * scale).dp))
            Text("Force Close", fontSize = (13 * scale).sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MiniInfoCard(
    title: String,
    value: String,
    iconRes: Int,
    modifier: Modifier = Modifier,
    scale: Float
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape((12 * scale).dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = (10 * scale).dp, vertical = (8 * scale).dp),
            horizontalArrangement = Arrangement.spacedBy((8 * scale).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size((14 * scale).dp)
            )
            Column {
                Text(
                    text = title,
                    fontSize = (10 * scale).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    fontSize = (12 * scale).sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StatusTicker(scale: Float) {
    val lines = remember {
        listOf(
            "Scanning LAN",
            "Syncing cluster",
            "Linking peers"
        )
    }
    var index by remember { mutableStateOf(0) }
    var text by remember { mutableStateOf("") }

    val pulse = rememberInfiniteTransition(label = "status-ticker")
    val dotScale by pulse.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "status-dot-scale"
    )

    LaunchedEffect(lines) {
        while (true) {
            val line = lines[index]
            for (i in 1..line.length) {
                text = line.substring(0, i)
                delay(45)
            }
            delay(800)
            index = (index + 1) % lines.size
            text = ""
            delay(140)
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size((7 * scale).dp)
                .scale(dotScale)
                .alpha(0.9f)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                )
        )
        Spacer(modifier = Modifier.size((7 * scale).dp))
        Text(
            text = text,
            fontSize = (12 * scale).sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}
