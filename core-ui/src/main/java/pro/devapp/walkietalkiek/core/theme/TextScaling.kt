package pro.devapp.walkietalkiek.core.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import kotlin.math.min

private const val MIN_SYSTEM_FONT_SCALE = 0.9f
private const val MAX_SYSTEM_FONT_SCALE = 1.25f
private const val BASELINE_WIDTH_DP = 411f
private const val MIN_RESPONSIVE_SCALE = 0.92f
private const val MAX_RESPONSIVE_SCALE = 1.10f

@Composable
fun rememberBoundedDensity(): Density {
    val density = LocalDensity.current
    val clampedFontScale = density.fontScale.coerceIn(MIN_SYSTEM_FONT_SCALE, MAX_SYSTEM_FONT_SCALE)
    return remember(density.density, clampedFontScale) {
        Density(density = density.density, fontScale = clampedFontScale)
    }
}

@Composable
fun rememberResponsiveTypography(base: Typography = Typography): Typography {
    val configuration = LocalConfiguration.current
    val smallestScreenDp = min(configuration.screenWidthDp, configuration.screenHeightDp)
        .coerceAtLeast(320)
    val responsiveScale = (smallestScreenDp / BASELINE_WIDTH_DP)
        .coerceIn(MIN_RESPONSIVE_SCALE, MAX_RESPONSIVE_SCALE)

    return remember(base, responsiveScale) {
        base.copy(
            displayLarge = base.displayLarge.scaleAndClamp(responsiveScale, 38f, 64f),
            displayMedium = base.displayMedium.scaleAndClamp(responsiveScale, 34f, 56f),
            displaySmall = base.displaySmall.scaleAndClamp(responsiveScale, 30f, 48f),
            headlineLarge = base.headlineLarge.scaleAndClamp(responsiveScale, 28f, 40f),
            headlineMedium = base.headlineMedium.scaleAndClamp(responsiveScale, 24f, 34f),
            headlineSmall = base.headlineSmall.scaleAndClamp(responsiveScale, 20f, 30f),
            titleLarge = base.titleLarge.scaleAndClamp(responsiveScale, 19f, 30f),
            titleMedium = base.titleMedium.scaleAndClamp(responsiveScale, 16f, 24f),
            titleSmall = base.titleSmall.scaleAndClamp(responsiveScale, 14f, 20f),
            bodyLarge = base.bodyLarge.scaleAndClamp(responsiveScale, 15f, 21f),
            bodyMedium = base.bodyMedium.scaleAndClamp(responsiveScale, 13f, 18f),
            bodySmall = base.bodySmall.scaleAndClamp(responsiveScale, 12f, 16f),
            labelLarge = base.labelLarge.scaleAndClamp(responsiveScale, 13f, 18f),
            labelMedium = base.labelMedium.scaleAndClamp(responsiveScale, 12f, 16f),
            labelSmall = base.labelSmall.scaleAndClamp(responsiveScale, 10f, 14f)
        )
    }
}

private fun TextStyle.scaleAndClamp(scale: Float, minSp: Float, maxSp: Float): TextStyle {
    val size = fontSize.scaledAndClamped(scale, minSp, maxSp)
    if (size == fontSize) return this

    val ratio = if (fontSize.isSpUnit() && lineHeight.isSpUnit() && fontSize.value > 0f) {
        lineHeight.value / fontSize.value
    } else {
        null
    }
    val adjustedLineHeight = ratio?.let { (size.value * it).sp } ?: lineHeight.scaled(scale)

    return copy(
        fontSize = size,
        lineHeight = adjustedLineHeight,
        letterSpacing = letterSpacing.scaled(scale)
    )
}

private fun TextUnit.scaledAndClamped(scale: Float, minSp: Float, maxSp: Float): TextUnit {
    if (!isSpUnit()) return this
    return (value * scale).coerceIn(minSp, maxSp).sp
}

private fun TextUnit.scaled(scale: Float): TextUnit {
    if (!isSpUnit()) return this
    return (value * scale).sp
}

private fun TextUnit.isSpUnit(): Boolean = type == TextUnitType.Sp
