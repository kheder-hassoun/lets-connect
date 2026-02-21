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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import pro.devapp.walkietalkiek.model.MainScreenState

@Composable
fun MainTopBar(
    modifier: Modifier = Modifier,
    state: MainScreenState,
    isPttEnabled: Boolean
) {
    val accent = MaterialTheme.colorScheme.primary
    val accentAlt = MaterialTheme.colorScheme.tertiary
    val isReady = state.requiredPermissions.isEmpty()
    val statusColor = when {
        !isReady -> Color(0xFFFF6B6B)
        !isPttEnabled -> Color(0xFFFFB74D)
        else -> Color(0xFF56E39F)
    }
    val statusLabel = when {
        !isReady -> "Action Needed"
        !isPttEnabled -> "PTT Offline"
        else -> "System Ready"
    }
    val currentTab = state.mainTabs.firstOrNull { it.screen == state.currentTab }
    val currentTabTitle = currentTab?.title ?: state.currentTab.name

    val pulseTransition = rememberInfiniteTransition(label = "topbar-status")
    val dotAlpha = pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "topbar-status-dot"
    ).value

    Surface(
        modifier = modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        tonalElevation = 8.dp,
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.18f),
                            accentAlt.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Let's Connect",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    TypewriterSubtitle(
                        modifier = Modifier.padding(top = 3.dp),
                        color = accent
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = accent.copy(alpha = 0.18f),
                        modifier = Modifier.border(
                            width = 1.dp,
                            color = accent.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(999.dp)
                        )
                    ) {
                        StatusChip(
                            label = statusLabel,
                            color = statusColor,
                            dotAlpha = dotAlpha
                        )
                    }

                    Spacer(modifier = Modifier.size(8.dp))

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = accent.copy(alpha = 0.18f),
                        modifier = Modifier.border(
                            width = 1.dp,
                            color = accent.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(999.dp)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            currentTab?.let {
                                Icon(
                                    painter = painterResource(id = it.icon),
                                    contentDescription = it.title,
                                    tint = accent,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = currentTabTitle,
                                style = MaterialTheme.typography.labelLarge,
                                color = accent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (!isReady) {
                        val pendingCount = state.requiredPermissions.size
                        Spacer(modifier = Modifier.size(8.dp))
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0x33FF6B6B)
                        ) {
                            Text(
                                text = "Pending: $pendingCount",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFFFB4A9),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypewriterSubtitle(
    modifier: Modifier = Modifier,
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

    val cursorTransition = rememberInfiniteTransition(label = "typewriter-cursor")
    val cursorAlpha by cursorTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(450),
            repeatMode = RepeatMode.Reverse
        ),
        label = "typewriter-cursor-alpha"
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
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = visibleText,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "|",
            style = MaterialTheme.typography.labelMedium,
            color = color.copy(alpha = cursorAlpha),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StatusChip(
    label: String,
    color: Color,
    dotAlpha: Float
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.16f),
        modifier = Modifier.border(
            width = 1.dp,
            color = color.copy(alpha = 0.45f),
            shape = RoundedCornerShape(999.dp)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = color.copy(alpha = dotAlpha),
                        shape = RoundedCornerShape(999.dp)
                    )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}
