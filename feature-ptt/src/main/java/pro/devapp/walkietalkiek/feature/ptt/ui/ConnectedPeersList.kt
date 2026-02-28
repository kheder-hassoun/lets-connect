package pro.devapp.walkietalkiek.feature.ptt.ui

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pro.devapp.walkietalkiek.serivce.network.data.model.ClientModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun ConnectedPeersList(
    devices: List<ClientModel>,
    modifier: Modifier = Modifier
) {
    val cfg = LocalConfiguration.current
    val screenWidth = cfg.screenWidthDp.dp
    val screenHeight = cfg.screenHeightDp.dp
    val base = screenWidth.coerceAtMost(screenHeight)
    val scale = (base / 400.dp).coerceIn(0.82f, 1.2f)
    val connectedPeers = devices.filter { it.isConnected }
    val connectedCount = connectedPeers.size
    val shape = RoundedCornerShape((14 * scale).dp)
    val radarSize = (screenHeight * 0.105f).coerceIn(74.dp, 136.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = (14 * scale).dp),
        shape = shape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x1A37D6FF),
                            Color(0x1017A34A),
                            Color.Transparent
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = shape
                )
                .padding(horizontal = (12 * scale).dp, vertical = (10 * scale).dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy((2 * scale).dp)
            ) {
                Text(
                    text = "Peers Available",
                    fontSize = (12 * scale).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy((8 * scale).dp)
                ) {
                    Text(
                        text = connectedCount.toString(),
                        fontSize = (26 * scale).sp,
                        color = if (connectedCount > 0) Color(0xFF3BD98A) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Scanning for nearby devices",
                        fontSize = (10 * scale).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            RadarView(
                modifier = Modifier.size(radarSize),
                peerCount = connectedCount
            )
        }
    }
}

@Composable
private fun RadarView(
    modifier: Modifier,
    peerCount: Int
) {
    val transition = rememberInfiniteTransition(label = "radar-transition")
    val sweepAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar-sweep"
    )
    val pulseScale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radar-pulse"
    )

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = (size.minDimension / 2f) * 0.92f * pulseScale
        val ringColor = Color(0xFF38BDF8)

        drawCircle(
            color = Color(0x1406B6D4),
            radius = radius
        )
        drawCircle(
            color = ringColor.copy(alpha = 0.32f),
            radius = radius,
            style = Stroke(width = size.minDimension * 0.015f)
        )
        drawCircle(
            color = ringColor.copy(alpha = 0.24f),
            radius = radius * 0.68f,
            style = Stroke(width = size.minDimension * 0.012f)
        )
        drawCircle(
            color = ringColor.copy(alpha = 0.2f),
            radius = radius * 0.36f,
            style = Stroke(width = size.minDimension * 0.011f)
        )
        drawLine(
            color = ringColor.copy(alpha = 0.2f),
            start = Offset(center.x - radius, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = size.minDimension * 0.008f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = ringColor.copy(alpha = 0.2f),
            start = Offset(center.x, center.y - radius),
            end = Offset(center.x, center.y + radius),
            strokeWidth = size.minDimension * 0.008f,
            cap = StrokeCap.Round
        )

        val sweepSize = Size(radius * 2f, radius * 2f)
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xAA22D3EE),
                    Color.Transparent
                ),
                center = center
            ),
            startAngle = sweepAngle - 20f,
            sweepAngle = 38f,
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = sweepSize
        )

        val pointCount = peerCount.coerceAtMost(12)
        repeat(pointCount) { index ->
            val angle = ((360f / pointCount) * index + 18f) * (PI.toFloat() / 180f)
            val ringFactor = 0.45f + ((index % 3) * 0.17f)
            val dotX = center.x + cos(angle) * radius * ringFactor
            val dotY = center.y + sin(angle) * radius * ringFactor
            drawCircle(
                color = Color(0xFF34D399),
                radius = size.minDimension * 0.035f,
                center = Offset(dotX, dotY)
            )
        }

        drawCircle(
            color = Color(0xFF06B6D4),
            radius = size.minDimension * 0.05f,
            center = center
        )
    }
}
