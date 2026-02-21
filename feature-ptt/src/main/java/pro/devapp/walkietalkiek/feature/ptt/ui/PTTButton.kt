package pro.devapp.walkietalkiek.feature.ptt.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pro.devapp.walkietalkiek.feature.ptt.R

@Composable
fun PTTButton(
    modifier: Modifier = Modifier,
    buttonSize: Dp = 228.dp,
    isOnline: Boolean = true,
    isRecording: Boolean = false,
    remainingSeconds: Int = 0,
    remainingMillis: Long = 0L,
    totalSeconds: Int = 10,
    onPress: () -> Unit = {},
    onRelease: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ptt-button")
    val waveShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave-shift"
    )

    val safeTotal = totalSeconds.coerceAtLeast(1)
    val totalMillis = safeTotal * 1000L
    val targetFraction = (remainingMillis.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)
    val fraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 120, easing = LinearEasing),
        label = "liquid-level"
    )
    val accent = MaterialTheme.colorScheme.primary
    val accentSoft = MaterialTheme.colorScheme.secondary
    val accentDeep = MaterialTheme.colorScheme.tertiary
    val liquidBaseColor = if (isOnline) accent.copy(alpha = 0.2f) else Color(0x22000000)
    val liquidGradient = Brush.verticalGradient(
        colors = listOf(
            accentSoft,
            accent,
            accentDeep
        )
    )
    val touchSize = buttonSize * 0.74f
    val iconPadding = (buttonSize * 0.12f).coerceAtLeast(18.dp)
    val timerBottomPadding = (buttonSize * 0.01f).coerceAtLeast(2.dp)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(buttonSize)) {
            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            val innerRadius = size.minDimension / 2.5f

            drawCircle(color = Color(0xFF181818), radius = innerRadius, center = center)

            val liquidLevel = center.y + innerRadius - (innerRadius * 2f * fraction)
            val waveAmplitude = 6.dp.toPx()
            val liquidPath = Path().apply {
                moveTo(center.x - innerRadius, center.y + innerRadius)
                lineTo(center.x - innerRadius, liquidLevel)
                var x = center.x - innerRadius
                while (x <= center.x + innerRadius) {
                    val normalized = (x - (center.x - innerRadius)) / (innerRadius * 2f)
                    val wave = kotlin.math.sin((normalized * Math.PI * 2).toFloat() + waveShift) * waveAmplitude
                    lineTo(x, liquidLevel + wave)
                    x += 4.dp.toPx()
                }
                lineTo(center.x + innerRadius, center.y + innerRadius)
                close()
            }

            val clipCircle = Path().apply {
                addOval(
                    Rect(
                        left = center.x - innerRadius,
                        top = center.y - innerRadius,
                        right = center.x + innerRadius,
                        bottom = center.y + innerRadius
                    )
                )
            }
            clipPath(clipCircle) {
                drawRect(color = liquidBaseColor)
                if (isRecording) {
                    drawPath(path = liquidPath, brush = liquidGradient, alpha = 0.95f)
                }
            }
            drawCircle(
                color = Color(0x55FFFFFF),
                radius = innerRadius,
                center = center,
                style = Stroke(width = 1.6.dp.toPx())
            )
        }

        Box(
            modifier = Modifier
                .size(touchSize)
                .pointerInput(isOnline) {
                    detectTapGestures(
                        onPress = {
                            if (!isOnline) return@detectTapGestures
                            try {
                                onPress()
                                awaitRelease()
                            } finally {
                                onRelease()
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ptt_call),
                contentDescription = "Push to talk",
                modifier = Modifier
                    .padding(iconPadding)
                    .fillMaxSize(),
                tint = if (isRecording) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }

        val timerLabel = if (isRecording) "$remainingSeconds s" else "$totalSeconds s"
        Text(
            text = timerLabel,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = timerBottomPadding),
            color = if (isRecording) accentSoft else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            style = if (isRecording) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
