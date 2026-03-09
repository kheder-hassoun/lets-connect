package pro.devapp.walkietalkiek.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import pro.devapp.walkietalkiek.core.settings.ThemeColor

private fun paletteFor(themeColor: ThemeColor): AccentPalette = when (themeColor) {
    ThemeColor.ORANGE -> OrangePalette
    ThemeColor.PURPLE -> PurplePalette
    ThemeColor.BLUE -> BluePalette
}

private fun buildDarkColorScheme(palette: AccentPalette) = darkColorScheme(
    primary = palette.primary,
    secondary = palette.secondary,
    tertiary = palette.tertiary,
    background = Black900,
    surface = Black800,
    surfaceVariant = Black700,
    primaryContainer = palette.primary.copy(alpha = 0.22f),
    secondaryContainer = palette.secondary.copy(alpha = 0.2f),
    tertiaryContainer = palette.tertiary.copy(alpha = 0.2f),
    outline = TextMuted.copy(alpha = 0.55f),
    onPrimaryContainer = TextLight,
    onSecondaryContainer = TextLight,
    onTertiaryContainer = TextLight,
    onSurfaceVariant = TextMuted,
    onPrimary = Black900,
    onSecondary = Black900,
    onTertiary = TextLight,
    onBackground = TextLight,
    onSurface = TextLight
)

private fun buildLightColorScheme(palette: AccentPalette) = lightColorScheme(
    primary = palette.primary,
    secondary = palette.secondary,
    tertiary = palette.tertiary,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    primaryContainer = palette.primary.copy(alpha = 0.18f),
    secondaryContainer = palette.secondary.copy(alpha = 0.16f),
    tertiaryContainer = palette.tertiary.copy(alpha = 0.16f),
    outline = TextDarkMuted.copy(alpha = 0.4f),
    onPrimary = TextLight,
    onSecondary = TextLight,
    onTertiary = TextLight,
    onPrimaryContainer = TextDark,
    onSecondaryContainer = TextDark,
    onTertiaryContainer = TextDark,
    onBackground = TextDark,
    onSurface = TextDark,
    onSurfaceVariant = TextDarkMuted
)

@Composable
fun DroidPTTTheme(
    darkTheme: Boolean = true,
    themeColor: ThemeColor = ThemeColor.ORANGE,
    typography: Typography = Typography,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val palette = paletteFor(themeColor)
    val colorScheme = if (darkTheme) {
        buildDarkColorScheme(palette)
    } else {
        buildLightColorScheme(palette)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
