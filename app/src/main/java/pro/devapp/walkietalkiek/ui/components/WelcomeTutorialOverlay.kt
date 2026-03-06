package pro.devapp.walkietalkiek.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
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
    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "welcome-wave-phase"
    )
    val gradientFlow = wavePhase

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val cardWidth = (screenWidth * 0.9f).coerceAtMost(560.dp)
        val cardHeight = rememberCardHeight(screenHeight)
        val tutorialGifSize = (screenWidth * 0.16f).coerceIn(46.dp, 86.dp)
        val primaryColor = MaterialTheme.colorScheme.primary

        Canvas(modifier = Modifier.fillMaxSize()) {
            val baseStart = lerp(
                Color(0xFF060B16),
                Color(0xFF0C1730),
                gradientFlow
            ).copy(alpha = 0.80f)
            val baseMid = lerp(
                Color(0xFF0C1A30),
                Color(0xFF152746),
                gradientFlow
            ).copy(alpha = 0.76f)
            val baseEnd = lerp(
                Color(0xFF101D34),
                Color(0xFF1B2D4D),
                gradientFlow
            ).copy(alpha = 0.72f)
            val diagStart = Offset(
                x = -size.width * 0.22f + (size.width * 0.30f * gradientFlow),
                y = -size.height * 0.22f + (size.height * 0.30f * gradientFlow)
            )
            val diagEnd = Offset(
                x = size.width * 1.05f + (size.width * 0.20f * gradientFlow),
                y = size.height * 1.05f + (size.height * 0.20f * gradientFlow)
            )

            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        baseStart,
                        baseMid,
                        baseEnd
                    ),
                    start = diagStart,
                    end = diagEnd
                )
            )
            // Diagonal light lane to make corner-to-corner gradient direction obvious.
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.12f + (0.12f * gradientFlow)),
                        Color(0xFF60A5FA).copy(alpha = 0.07f + (0.08f * gradientFlow)),
                        Color.Transparent
                    ),
                    start = diagStart,
                    end = diagEnd
                )
            )

            val maxRadius = size.maxDimension * 1.55f
            val minRadius = size.maxDimension * 0.10f
            val waveCenter = Offset(-size.width * 0.08f, -size.height * 0.08f)
            val ringCount = 18
            val spacing = 1f / ringCount
            for (i in 0 until ringCount) {
                val shift = (wavePhase + (i * spacing)) % 1f
                // Ease-out expansion: fast near the corner, slower as rings grow.
                val eased = 1f - (1f - shift).let { it * it }
                val radius = minRadius + (maxRadius - minRadius) * eased
                val waveAlpha = (1f - eased) * 0.34f
                val strokeWidth = (2.4f + (eased * 2.8f)).dp.toPx()
                val waveBrush = Brush.linearGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = waveAlpha),
                        Color(0xFF60A5FA).copy(alpha = waveAlpha * 0.88f),
                        Color(0xFF818CF8).copy(alpha = waveAlpha * 0.76f),
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )

                drawCircle(
                    brush = waveBrush,
                    radius = radius,
                    center = waveCenter,
                    style = Stroke(width = strokeWidth)
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
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedBrandGifIcon(
                            size = tutorialGifSize
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.welcome_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = stringResource(
                                R.string.welcome_step_counter,
                                currentStep + 1,
                                steps.size
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

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
                            val stepTitle = stringResource(step.titleRes)
                            val stepBody = stringResource(step.bodyRes)
                            val bodyHighlightPhrase = if (index == steps.lastIndex) {
                                stringResource(R.string.welcome_step_3_highlight_phrase)
                            } else {
                                null
                            }
                            val titleCharDelayMs = 30
                            val bodyStartDelayMs = (stepTitle.length * titleCharDelayMs) + 260
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
                                    text = stepTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = Color.White,
                                    charDelayMs = titleCharDelayMs
                                )
                                TypewriterText(
                                    text = stepBody,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f),
                                    textAlign = TextAlign.Center,
                                    startDelayMs = bodyStartDelayMs,
                                    charDelayMs = 24,
                                    highlightPhrase = bodyHighlightPhrase,
                                    highlightColor = MaterialTheme.colorScheme.primary
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
    fontWeight: FontWeight? = null,
    startDelayMs: Int = 0,
    charDelayMs: Int = 26,
    highlightPhrase: String? = null,
    highlightColor: Color = color
) {
    var visibleChars by remember(text, startDelayMs, charDelayMs) { mutableIntStateOf(0) }
    val cursorAlpha by rememberInfiniteTransition(label = "typewriter-cursor").animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 620, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "typewriter-cursor-alpha"
    )

    LaunchedEffect(text, startDelayMs, charDelayMs) {
        visibleChars = 0
        if (startDelayMs > 0) delay(startDelayMs.toLong())
        for (i in 1..text.length) {
            visibleChars = i
            delay(charDelayMs.toLong())
        }
    }

    Text(
        text = buildAnnotatedString {
            val visibleText = text.take(visibleChars)
            append(visibleText)

            val phrase = highlightPhrase?.trim().orEmpty()
            if (phrase.isNotEmpty()) {
                val highlightStart = text.indexOf(phrase)
                if (highlightStart >= 0 && visibleChars > highlightStart) {
                    val highlightEnd = (highlightStart + phrase.length).coerceAtMost(visibleChars)
                    addStyle(
                        style = SpanStyle(color = highlightColor),
                        start = highlightStart,
                        end = highlightEnd
                    )
                }
            }

            append(" ")
            pushStyle(SpanStyle(color = color.copy(alpha = cursorAlpha)))
            append("|")
            pop()
        },
        style = style,
        color = color,
        textAlign = textAlign,
        fontWeight = fontWeight
    )
}
