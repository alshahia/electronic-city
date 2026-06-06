package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ShopDarkGreenPrimary,
    secondary = ShopGreenSecondary,
    tertiary = ShopGreenContainer,
    background = ShopDarkBackground,
    surface = ShopDarkSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFFE2EADF),
    onSurface = Color(0xFFE2EADF),
    primaryContainer = Color(0xFF233A18),      // Deep olive-green bubble for dark theme
    onPrimaryContainer = Color(0xFFEAF5DD),    // Pastel green text
    surfaceVariant = Color(0xFF1D2F18),        // Slightly lighter dark olive for cards
    outline = Color(0xFF304625)                // Mmuted line outline for dark cards
)

private val LightColorScheme = lightColorScheme(
    primary = ShopGreenPrimary,
    secondary = ShopGreenSecondary,
    tertiary = ShopGreenDark,
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    primaryContainer = ShopGreenContainer,
    onPrimaryContainer = ShopOnGreenContainer,
    surfaceVariant = Color(0xFFEEF3E6),        // Subtle differentiation from background (D8.9)
    outline = BorderLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We disable dynamicColor by default to guarantee our branded "E-City" Arabic Green is perfectly preserved!
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = remember(darkTheme) {
        if (darkTheme) DarkColorScheme else LightColorScheme
    }
    val typography = remember { AppTypography }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
