package pro.devapp.walkietalkiek.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = OrangePrimary,
    secondary = OrangeAccent,
    tertiary = OrangePrimaryMuted,
    background = Black900,
    surface = Black800,
    surfaceVariant = Black700,
    onPrimary = Black900,
    onSecondary = Black900,
    onTertiary = TextLight,
    onBackground = TextLight,
    onSurface = TextLight
)

private val LightColorScheme = lightColorScheme(
    primary = OrangePrimary,
    secondary = OrangePrimaryMuted,
    tertiary = OrangeAccent,
    background = Black850,
    surface = Black700,
    surfaceVariant = Black800,
    onPrimary = Black900,
    onSecondary = TextLight,
    onTertiary = Black900,
    onBackground = TextLight,
    onSurface = TextLight
)

@Composable
fun DroidPTTTheme(
    darkTheme: Boolean = true,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
//        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//            val context = LocalContext.current
//            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
