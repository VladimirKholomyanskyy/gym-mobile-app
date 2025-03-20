package com.neyra.gymapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.neyra.gymapp.R

val CyberpunkFontFamily = FontFamily(
    Font(R.font.audiowide_regular)
)

// Define your typography using the cyberpunk font and colors
val Typography = Typography(
    // Display styles for large headers
    displayLarge = TextStyle(
        fontFamily = CyberpunkFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = 0.sp,
        color = TextPrimary
    ),
    displayMedium = TextStyle(
        fontFamily = CyberpunkFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
        color = TextPrimary
    ),
    displaySmall = TextStyle(
        fontFamily = CyberpunkFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
        color = TextPrimary
    ),

    // Headline styles for section headers
    headlineLarge = TextStyle(
        fontFamily = CyberpunkFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
        color = AccentMagenta
    ),
    headlineMedium = TextStyle(
        fontFamily = CyberpunkFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
        color = AccentMagenta
    ),
    headlineSmall = TextStyle(
        fontFamily = CyberpunkFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
        color = AccentNeonBlue
    ),

    // Title styles for items and cards
    titleLarge = TextStyle(
        fontFamily = CyberpunkFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
        color = AccentMagenta
    ),
    titleMedium = TextStyle(
        fontFamily = CyberpunkFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
        color = TextPrimary
    ),
    titleSmall = TextStyle(
        fontFamily = CyberpunkFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
        color = TextPrimary
    ),

    // Body styles for content
    bodyLarge = TextStyle(
        fontFamily = CyberpunkFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
        color = TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = CyberpunkFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
        color = TextSecondary
    ),
    bodySmall = TextStyle(
        fontFamily = CyberpunkFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
        color = TextSecondary
    ),

    // Label styles for buttons and small text
    labelLarge = TextStyle(
        fontFamily = CyberpunkFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp,
        color = TextPrimary
    ),
    labelMedium = TextStyle(
        fontFamily = CyberpunkFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        color = TextPrimary
    ),
    labelSmall = TextStyle(
        fontFamily = CyberpunkFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        color = TextSecondary
    )
)