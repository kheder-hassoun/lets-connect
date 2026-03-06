package pro.devapp.walkietalkiek.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import pro.devapp.walkietalkiek.R

@Composable
internal fun WelcomeTutorialOverlay(
    onFinish: (neverShowAgain: Boolean) -> Unit
) {
    val steps = remember {
        listOf(
            TutorialStepUi(
                iconRes = R.drawable.settings,
                titleRes = R.string.welcome_step_1_title,
                bodyRes = R.string.welcome_step_1_body
            ),
            TutorialStepUi(
                iconRes = R.drawable.connection_on,
                titleRes = R.string.welcome_step_2_title,
                bodyRes = R.string.welcome_step_2_body
            ),
            TutorialStepUi(
                iconRes = R.drawable.ptt,
                titleRes = R.string.welcome_step_3_title,
                bodyRes = R.string.welcome_step_3_body
            )
        )
    }
    var currentStep by remember { mutableIntStateOf(0) }
    var neverShowAgain by remember { mutableStateOf(false) }
    val isLastStep = currentStep == steps.lastIndex

    val transition = rememberInfiniteTransition(label = "welcome-animations")
    val pulseScale by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "welcome-pulse-scale"
    )
    val borderShiftAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "welcome-border-shift"
    )
    val scrimAlpha by transition.animateFloat(
        initialValue = 0.42f,
        targetValue = 0.58f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "welcome-scrim-alpha"
    )
    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "welcome-wave-phase"
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val cardWidth = (screenWidth * 0.9f).coerceAtMost(560.dp)
        val cardHeight = rememberCardHeight(screenHeight)
        val primaryColor = MaterialTheme.colorScheme.primary

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = scrimAlpha),
                        Color(0xFF0A1424).copy(alpha = (scrimAlpha + 0.08f).coerceAtMost(0.76f))
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )
            )

            val maxRadius = size.maxDimension * 1.15f
            val minRadius = size.maxDimension * 0.12f
            for (i in 0..2) {
                val shift = (wavePhase + (i * 0.33f)) % 1f
                val radius = minRadius + (maxRadius - minRadius) * shift
                val waveAlpha = (1f - shift) * 0.14f

                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = waveAlpha),
                            Color(0xFF64B5F6).copy(alpha = waveAlpha * 0.84f),
                            Color.Transparent
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(radius, radius)
                    ),
                    radius = radius,
                    center = Offset(0f, 0f),
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF64B5F6).copy(alpha = waveAlpha * 0.92f),
                            primaryColor.copy(alpha = waveAlpha * 0.82f),
                            Color.Transparent
                        ),
                        start = Offset(size.width, size.height),
                        end = Offset(size.width - radius, size.height - radius)
                    ),
                    radius = radius,
                    center = Offset(size.width, size.height),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .widthIn(max = cardWidth)
                .fillMaxWidth()
                .height(cardHeight)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 3.dp.toPx()
                val glowStroke = 9.dp.toPx()
                val inset = glowStroke
                val corner = 24.dp.toPx()
                val angleRad = (borderShiftAngle * (Math.PI / 180f)).toFloat()
                val travelRadius = size.maxDimension * 0.5f
                val gradientStart = Offset(
                    x = center.x + kotlin.math.cos(angleRad) * travelRadius,
                    y = center.y + kotlin.math.sin(angleRad) * travelRadius
                )
                val gradientEnd = Offset(
                    x = center.x - kotlin.math.cos(angleRad) * travelRadius,
                    y = center.y - kotlin.math.sin(angleRad) * travelRadius
                )
                val animatedBorderBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF90CAF9),
                        Color(0xFF64B5F6),
                        Color(0xFF7986CB),
                        Color(0xFF9575CD),
                        Color(0xFF7E57C2),
                        Color(0xFF5C6BC0)
                    ),
                    start = gradientStart,
                    end = gradientEnd
                )

                drawRoundRect(
                    brush = animatedBorderBrush,
                    topLeft = Offset(inset, inset),
                    size = Size(
                        width = size.width - (inset * 2f),
                        height = size.height - (inset * 2f)
                    ),
                    cornerRadius = CornerRadius(corner, corner),
                    style = Stroke(width = stroke)
                )

                drawRoundRect(
                    brush = animatedBorderBrush,
                    topLeft = Offset(inset, inset),
                    size = Size(
                        width = size.width - (inset * 2f),
                        height = size.height - (inset * 2f)
                    ),
                    cornerRadius = CornerRadius(corner, corner),
                    style = Stroke(width = glowStroke),
                    alpha = 0.15f
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF111827).copy(alpha = 0.97f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.welcome_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = { onFinish(false) },
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                    shape = CircleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
                                    shape = CircleShape
                                )
                        ) {
                            Text(
                                text = "X",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        text = stringResource(
                            R.string.welcome_step_counter,
                            currentStep + 1,
                            steps.size
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = currentStep,
                            label = "welcome-step-content"
                        ) { index ->
                            val step = steps[index]
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(74.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                            shape = CircleShape
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(58.dp)
                                            .scale(pulseScale)
                                            .alpha(0.2f)
                                            .background(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = CircleShape
                                            )
                                    )
                                    Icon(
                                        painter = painterResource(step.iconRes),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                TypewriterText(
                                    text = stringResource(step.titleRes),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = Color.White
                                )
                                TypewriterText(
                                    text = stringResource(step.bodyRes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                            }
                        }
                    }

                    if (isLastStep) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = neverShowAgain,
                                onCheckedChange = { neverShowAgain = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                            )
                            Text(
                                text = stringResource(R.string.welcome_never_show_again),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(4.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = { currentStep = (currentStep - 1).coerceAtLeast(0) },
                            enabled = currentStep > 0
                        ) {
                            Text(stringResource(R.string.welcome_previous))
                        }
                        Button(
                            onClick = {
                                if (isLastStep) {
                                    onFinish(neverShowAgain)
                                } else {
                                    currentStep = (currentStep + 1).coerceAtMost(steps.lastIndex)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = if (isLastStep) {
                                    stringResource(R.string.welcome_done)
                                } else {
                                    stringResource(R.string.welcome_next)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberCardHeight(screenHeight: Dp): Dp {
    val idealHeight = screenHeight * 0.72f
    return idealHeight.coerceIn(minimumValue = 460.dp, maximumValue = 760.dp)
}

private data class TutorialStepUi(
    val iconRes: Int,
    val titleRes: Int,
    val bodyRes: Int
)

@Composable
private fun TypewriterText(
    text: String,
    style: TextStyle,
    color: Color,
    textAlign: TextAlign = TextAlign.Start,
    fontWeight: FontWeight? = null
) {
    var visibleChars by remember(text) { mutableIntStateOf(0) }

    LaunchedEffect(text) {
        visibleChars = 0
        for (i in 1..text.length) {
            visibleChars = i
            delay(14)
        }
    }

    Text(
        text = text.take(visibleChars),
        style = style,
        color = color,
        textAlign = textAlign,
        fontWeight = fontWeight
    )
}
