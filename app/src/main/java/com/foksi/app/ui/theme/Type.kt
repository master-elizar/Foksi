package com.foksi.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Generous, readable type. Everything scales with the user's chosen size so the app stays
 * comfortable for people who need larger text.
 */
fun foksiTypography(scale: Float): Typography {
    fun sp(value: Float): TextUnit = (value * scale).sp
    val base = FontFamily.Default
    return Typography(
        displaySmall = TextStyle(
            fontFamily = base, fontWeight = FontWeight.SemiBold,
            fontSize = sp(34f), lineHeight = sp(42f)
        ),
        headlineLarge = TextStyle(
            fontFamily = base, fontWeight = FontWeight.SemiBold,
            fontSize = sp(30f), lineHeight = sp(38f)
        ),
        headlineMedium = TextStyle(
            fontFamily = base, fontWeight = FontWeight.SemiBold,
            fontSize = sp(25f), lineHeight = sp(32f)
        ),
        headlineSmall = TextStyle(
            fontFamily = base, fontWeight = FontWeight.SemiBold,
            fontSize = sp(21f), lineHeight = sp(28f)
        ),
        titleLarge = TextStyle(
            fontFamily = base, fontWeight = FontWeight.SemiBold,
            fontSize = sp(19f), lineHeight = sp(26f)
        ),
        titleMedium = TextStyle(
            fontFamily = base, fontWeight = FontWeight.Medium,
            fontSize = sp(16f), lineHeight = sp(23f)
        ),
        titleSmall = TextStyle(
            fontFamily = base, fontWeight = FontWeight.Medium,
            fontSize = sp(14f), lineHeight = sp(20f)
        ),
        bodyLarge = TextStyle(
            fontFamily = base, fontWeight = FontWeight.Normal,
            fontSize = sp(16f), lineHeight = sp(24f)
        ),
        bodyMedium = TextStyle(
            fontFamily = base, fontWeight = FontWeight.Normal,
            fontSize = sp(14.5f), lineHeight = sp(21f)
        ),
        bodySmall = TextStyle(
            fontFamily = base, fontWeight = FontWeight.Normal,
            fontSize = sp(13f), lineHeight = sp(18f)
        ),
        labelLarge = TextStyle(
            fontFamily = base, fontWeight = FontWeight.Medium,
            fontSize = sp(14f), lineHeight = sp(20f)
        ),
        labelMedium = TextStyle(
            fontFamily = base, fontWeight = FontWeight.Medium,
            fontSize = sp(12.5f), lineHeight = sp(17f)
        ),
        labelSmall = TextStyle(
            fontFamily = base, fontWeight = FontWeight.Medium,
            fontSize = sp(11.5f), lineHeight = sp(16f)
        ),
    )
}
