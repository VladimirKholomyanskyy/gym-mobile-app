package com.neyra.gymapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Create a custom cyberpunk dark color scheme
private val CyberpunkDarkColorScheme = darkColorScheme(
    // Primary colors
    primary = AccentMagenta,
    onPrimary = TextPrimary,
    primaryContainer = BackgroundSecondary,
    onPrimaryContainer = TextPrimary,

    // Secondary colors
    secondary = AccentNeonBlue,
    onSecondary = TextPrimary,
    secondaryContainer = BackgroundSecondary,
    onSecondaryContainer = TextPrimary,

    // Tertiary colors
    tertiary = AccentNeonGreen,
    onTertiary = CardBackground,
    tertiaryContainer = BackgroundSecondary,
    onTertiaryContainer = TextPrimary,

    // Background colors
    background = BackgroundPrimary,
    onBackground = TextPrimary,
    surface = CardBackground,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundSecondary,
    onSurfaceVariant = TextSecondary,

    // Error colors
    error = StatusError,
    onError = TextPrimary,
    errorContainer = BackgroundSecondary,
    onErrorContainer = StatusError,

    // Other colors
    outline = TextSecondary,
    outlineVariant = TextDisabled,
    scrim = CardBackground.copy(alpha = 0.5f)
)

// Create a local composition for our custom cyberpunk colors
// This allows us to access additional custom colors that aren't part of Material3 ColorScheme
data class CyberpunkColors(
    val highlight: Color = AccentPurple,
    val warning: Color = StatusWarning,
    val success: Color = StatusSuccess,
    val cardBackground: Color = CardBackground,
    val glowAccent: Color = AccentMagenta.copy(alpha = 0.6f)
)

val LocalCyberpunkColors = staticCompositionLocalOf { CyberpunkColors() }

@Composable
fun GymAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is not used in cyberpunk theme - we want our specific colors
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // We always use dark theme for cyberpunk aesthetic
    val colorScheme = CyberpunkDarkColorScheme

    // Define our custom cyberpunk colors
    val cyberpunkColors = CyberpunkColors(
        highlight = AccentPurple,
        warning = StatusWarning,
        success = StatusSuccess,
        cardBackground = CardBackground,
        glowAccent = AccentMagenta.copy(alpha = 0.6f)
    )

    CompositionLocalProvider(
        LocalCyberpunkColors provides cyberpunkColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// Extension function to access cyberpunk specific colors
object CyberpunkTheme {
    val colors: CyberpunkColors
        @Composable
        get() = LocalCyberpunkColors.current
}