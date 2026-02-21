package pro.devapp.walkietalkiek.core.theme

import androidx.compose.ui.graphics.Color

data class AccentPalette(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color
)

val PurplePalette = AccentPalette(
    primary = Color(0xFFA970FF),
    secondary = Color(0xFFC49BFF),
    tertiary = Color(0xFF7E4EDC)
)

val OrangePalette = AccentPalette(
    primary = Color(0xFFFF8A00),
    secondary = Color(0xFFFFB347),
    tertiary = Color(0xFFCC6E00)
)

val RedPalette = AccentPalette(
    primary = Color(0xFFFF5252),
    secondary = Color(0xFFFF8A80),
    tertiary = Color(0xFFCC3A3A)
)

val BluePalette = AccentPalette(
    primary = Color(0xFF4DA3FF),
    secondary = Color(0xFF82C4FF),
    tertiary = Color(0xFF2979CC)
)

val GreenPalette = AccentPalette(
    primary = Color(0xFF4CD964),
    secondary = Color(0xFF85F39B),
    tertiary = Color(0xFF2EA84A)
)

val YellowPalette = AccentPalette(
    primary = Color(0xFFFFD54F),
    secondary = Color(0xFFFFE082),
    tertiary = Color(0xFFFFB300)
)

val Black900 = Color(0xFF050505)
val Black800 = Color(0xFF121212)
val Black700 = Color(0xFF1A1A1A)

val TextLight = Color(0xFFF5F5F5)
val TextMuted = Color(0xFFB8B8B8)
