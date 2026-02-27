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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pro.devapp.walkietalkiek.feature.ptt.R

@Composable
fun PTTButton(
    modifier: Modifier = Modifier,
    buttonSize: Dp = 228.dp,
    isOnline: Boolean = true,
    isEnabled: Boolean = true,
    isLockedByRemote: Boolean = false,
    isRemoteSpeaking: Boolean = false,
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
    val borderRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "neon-border-rotation"
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
    val liquidBaseColor = when {
        isRemoteSpeaking -> Color(0xFF2B2B2B)
        isOnline -> accent.copy(alpha = 0.2f)
        else -> Color(0x22000000)
    }
    val liquidGradient = Brush.verticalGradient(
        colors = listOf(
            accentSoft,
            accent,
            accentDeep
        )
    )
    val neonSweep = Brush.sweepGradient(
        colors = if (isRemoteSpeaking) {
            listOf(
                Color(0xFF5A5A5A),
                Color(0xFF6C6C6C),
                Color(0xFF4A4A4A),
                Color(0xFF5A5A5A)
            )
        } else {
            listOf(
                Color(0xFF90CAF9),
                Color(0xFF64B5F6),
                Color(0xFF7986CB),
                Color(0xFF9575CD),
                Color(0xFF7E57C2),
                Color(0xFF5C6BC0),
                Color(0xFF90CAF9)
            )
        }
    )
    val remoteWaveGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF7A7A7A),
            Color(0xFF5A5A5A),
            Color(0xFF3A3A3A)
        )
    )
    val touchSize = buttonSize * 0.74f
    val iconPadding = (buttonSize * 0.12f).coerceAtLeast(18.dp)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(buttonSize)) {
            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            val innerRadius = size.minDimension / 2.5f

            drawCircle(color = Color(0xFF181818), radius = innerRadius, center = center)

            val remoteWaveLevel = 0.48f + (kotlin.math.sin(waveShift * 0.6f) * 0.14f)
            val levelFraction = when {
                isRecording -> fraction
                isRemoteSpeaking -> remoteWaveLevel.coerceIn(0.22f, 0.78f)
                else -> 1f
            }
            val liquidLevel = center.y + innerRadius - (innerRadius * 2f * levelFraction)
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
                if (isRecording || isRemoteSpeaking) {
                    drawPath(
                        path = liquidPath,
                        brush = if (isRemoteSpeaking) remoteWaveGradient else liquidGradient,
                        alpha = if (isRemoteSpeaking) 0.82f else 0.95f
                    )
                }
            }
            drawCircle(
                color = Color(0x55FFFFFF),
                radius = innerRadius,
                center = center,
                style = Stroke(width = 1.6.dp.toPx())
            )

            rotate(degrees = borderRotation, pivot = center) {
                drawCircle(
                    brush = neonSweep,
                    radius = innerRadius,
                    center = center,
                    style = Stroke(width = 2.4.dp.toPx())
                )
            }
            drawCircle(
                brush = neonSweep,
                radius = innerRadius,
                center = center,
                style = Stroke(width = 4.2.dp.toPx()),
                alpha = 0.14f
            )
        }

        Box(
            modifier = Modifier
                .size(touchSize)
                .pointerInput(isEnabled) {
                    detectTapGestures(
                        onPress = {
                            if (!isEnabled) return@detectTapGestures
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
                painter = painterResource(
                    id = if (isRemoteSpeaking) R.drawable.speaker else R.drawable.ptt_call
                ),
                contentDescription = "Push to talk",
                modifier = Modifier
                    .padding(iconPadding)
                    .fillMaxSize(),
                tint = when {
                    isRecording -> MaterialTheme.colorScheme.onPrimary
                    isRemoteSpeaking -> Color(0xFFBDBDBD)
                    !isEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}
