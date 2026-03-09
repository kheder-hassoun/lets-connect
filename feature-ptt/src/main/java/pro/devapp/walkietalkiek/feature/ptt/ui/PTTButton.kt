package pro.devapp.walkietalkiek.feature.ptt.ui

import android.widget.ImageView
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import pro.devapp.walkietalkiek.feature.ptt.R
import pl.droidsonroids.gif.GifImageView

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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val idleBase = if (isDark) Color(0xFF111827) else Color(0xFFF9FAFB)
    val disabledBase = if (isDark) Color(0xFF1F2937) else Color(0xFFE5E7EB)
    val holdingBase = if (isDark) Color(0xFF7C2D12) else Color(0xFFFFEDD5)
    val holdingGradient = if (isDark) {
        listOf(Color(0xFFF59E0B), Color(0xFFEA580C), Color(0xFFC2410C))
    } else {
        listOf(Color(0xFFFBBF24), Color(0xFFF97316), Color(0xFFEA580C))
    }
    val remoteGradient = if (isDark) {
        listOf(Color(0xFFFCD34D), Color(0xFFFBBF24), Color(0xFFF59E0B))
    } else {
        listOf(Color(0xFFFBBF24), Color(0xFFF59E0B), Color(0xFFD97706))
    }

    val isDisabled = !isOnline || !isEnabled
    val isRemoteBusy = isRemoteSpeaking || isLockedByRemote
    val innerSurface = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    val liquidBaseColor = when {
        isDisabled -> disabledBase
        isRemoteBusy -> if (isDark) Color(0xFF6B4A1E) else Color(0xFFFDE68A)
        isRecording -> holdingBase
        isOnline -> idleBase
        else -> disabledBase
    }
    val liquidGradient = Brush.verticalGradient(
        colors = holdingGradient
    )
    val ringWarmGradient = listOf(
        Color(0xFFFFB74D),
        Color(0xFFFF9800),
        Color(0xFFFF7043),
        Color(0xFFEF5350),
        Color(0xFFE53935),
        Color(0xFFFF8A65),
        Color(0xFFFFB74D)
    )
    val neonSweep = Brush.sweepGradient(
        colors = when {
            isDisabled -> listOf(
                disabledBase.copy(alpha = 0.65f),
                disabledBase.copy(alpha = 0.9f),
                disabledBase.copy(alpha = 0.65f)
            )
            isRemoteBusy -> listOf(
                remoteGradient[0],
                remoteGradient[1],
                remoteGradient[2],
                remoteGradient[0]
            )
            else -> ringWarmGradient
        }
    )
    val remoteWaveGradient = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(
                Color(0xFFFCD34D),
                Color(0xFFFBBF24),
                Color(0xFFF59E0B)
            )
        } else {
            listOf(
                Color(0xFFFDE68A),
                Color(0xFFFBBF24),
                Color(0xFFD97706)
            )
        }
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

            drawCircle(
                color = innerSurface,
                radius = innerRadius,
                center = center
            )

            val remoteWaveLevel = 0.48f + (kotlin.math.sin(waveShift * 0.6f) * 0.14f)
            val levelFraction = when {
                isRecording -> fraction
                isRemoteBusy -> remoteWaveLevel.coerceIn(0.22f, 0.78f)
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
                if (isRecording || isRemoteBusy) {
                    drawPath(
                        path = liquidPath,
                        brush = if (isRemoteBusy) remoteWaveGradient else liquidGradient,
                        alpha = if (isRemoteBusy) 0.86f else 0.95f
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
            if (isRemoteSpeaking) {
                Icon(
                    painter = painterResource(id = R.drawable.speaker),
                    contentDescription = stringResource(R.string.ptt_button_content_description),
                    modifier = Modifier
                        .padding(iconPadding)
                        .fillMaxSize(),
                    tint = Color(0xFFBDBDBD)
                )
            } else {
                AndroidView(
                    factory = { ctx ->
                        GifImageView(ctx).apply {
                            setImageResource(R.drawable.ptt_icone)
                            scaleType = ImageView.ScaleType.FIT_CENTER
                        }
                    },
                    update = { gifView ->
                        gifView.alpha = if (isEnabled) 1f else 0.45f
                    },
                    modifier = Modifier
                        .padding(iconPadding)
                        .fillMaxSize()
                )
            }
        }
    }
}
