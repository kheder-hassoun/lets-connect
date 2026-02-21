package pro.devapp.walkietalkiek.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import pro.devapp.walkietalkiek.core.settings.ThemeColor

private fun paletteFor(themeColor: ThemeColor): AccentPalette = when (themeColor) {
    ThemeColor.PURPLE -> PurplePalette
    ThemeColor.ORANGE -> OrangePalette
    ThemeColor.RED -> RedPalette
    ThemeColor.BLUE -> BluePalette
    ThemeColor.GREEN -> GreenPalette
    ThemeColor.YELLOW -> YellowPalette
}

private fun buildDarkColorScheme(palette: AccentPalette) = darkColorScheme(
    primary = palette.primary,
    secondary = palette.secondary,
    tertiary = palette.tertiary,
    background = Black900,
    surface = Black800,
    surfaceVariant = Black700,
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
    background = Black900,
    surface = Black800,
    surfaceVariant = Black700,
    onPrimary = Black900,
    onSecondary = Black900,
    onTertiary = TextLight,
    onBackground = TextLight,
    onSurface = TextLight
)

@Composable
fun DroidPTTTheme(
    darkTheme: Boolean = true,
    themeColor: ThemeColor = ThemeColor.PURPLE,
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
        typography = Typography,
        content = content
    )
}
