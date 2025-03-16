package com.neyra.gymapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.neyra.gymapp.R

val AudiowideFontFamily = FontFamily(
    Font(R.font.audiowide_regular)
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = AudiowideFontFamily,
        fontSize = 40.sp,
        color = TextPrimary
    ),
    displayMedium = TextStyle(
        fontFamily = AudiowideFontFamily,
        fontSize = 30.sp,
        color = TextPrimary
    ),
    displaySmall = TextStyle(
        fontFamily = AudiowideFontFamily,
        fontSize = 24.sp,
        color = TextSecondary
    ),
    bodyLarge = TextStyle(
        fontFamily = AudiowideFontFamily,
        fontSize = 16.sp,
        color = TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = AudiowideFontFamily,
        fontSize = 14.sp,
        color = TextSecondary
    )
)

//// Set of Material typography styles to start with
//val Typography = Typography(
//    bodyLarge = TextStyle(
//        fontFamily = FontFamily.Default,
//        fontWeight = FontWeight.Normal,
//        fontSize = 16.sp,
//        lineHeight = 24.sp,
//        letterSpacing = 0.5.sp
//    )
//    /* Other default text styles to override
//    titleLarge = TextStyle(
//        fontFamily = FontFamily.Default,
//        fontWeight = FontWeight.Normal,
//        fontSize = 22.sp,
//        lineHeight = 28.sp,
//        letterSpacing = 0.sp
//    ),
//    labelSmall = TextStyle(
//        fontFamily = FontFamily.Default,
//        fontWeight = FontWeight.Medium,
//        fontSize = 11.sp,
//        lineHeight = 16.sp,
//        letterSpacing = 0.5.sp
//    )
//    */
//)