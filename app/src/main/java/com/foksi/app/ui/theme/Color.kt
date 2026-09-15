package com.foksi.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.foksi.app.domain.model.PaletteId

/**
 * Soft, low-saturation palettes inspired by modern Linux desktops: warm paper backgrounds,
 * one confident accent, and no neon anywhere.
 */
object FoksiPalettes {

    private val InkLight = Color(0xFF1F1B18)
    private val InkMutedLight = Color(0xFF6F6259)
    private val InkDark = Color(0xFFF3EAE3)
    private val InkMutedDark = Color(0xFFB7A79B)

    private fun light(
        primary: Color,
        container: Color,
        onContainer: Color,
        accent: Color,
        background: Color,
        surface: Color,
        surfaceVariant: Color,
        outline: Color,
    ): ColorScheme = lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = container,
        onPrimaryContainer = onContainer,
        secondary = accent,
        onSecondary = Color.White,
        secondaryContainer = container,
        onSecondaryContainer = onContainer,
        tertiary = accent,
        onTertiary = Color.White,
        tertiaryContainer = container,
        onTertiaryContainer = onContainer,
        background = background,
        onBackground = InkLight,
        surface = surface,
        onSurface = InkLight,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = InkMutedLight,
        surfaceTint = primary,
        outline = outline,
        outlineVariant = surfaceVariant,
        inverseSurface = InkLight,
        inverseOnSurface = background,
        error = Color(0xFFB3261E),
        onError = Color.White,
        errorContainer = Color(0xFFF9DEDC),
        onErrorContainer = Color(0xFF410E0B),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = surface,
        surfaceContainer = background,
        surfaceContainerHigh = surfaceVariant,
        surfaceContainerHighest = surfaceVariant,
        surfaceBright = Color.White,
        surfaceDim = surfaceVariant,
    )

    private fun dark(
        primary: Color,
        container: Color,
        onContainer: Color,
        accent: Color,
        background: Color,
        surface: Color,
        surfaceVariant: Color,
        outline: Color,
    ): ColorScheme = darkColorScheme(
        primary = primary,
        onPrimary = Color(0xFF221009),
        primaryContainer = container,
        onPrimaryContainer = onContainer,
        secondary = accent,
        onSecondary = Color(0xFF221009),
        secondaryContainer = container,
        onSecondaryContainer = onContainer,
        tertiary = accent,
        onTertiary = Color(0xFF221009),
        tertiaryContainer = container,
        onTertiaryContainer = onContainer,
        background = background,
        onBackground = InkDark,
        surface = surface,
        onSurface = InkDark,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = InkMutedDark,
        surfaceTint = primary,
        outline = outline,
        outlineVariant = surfaceVariant,
        inverseSurface = InkDark,
        inverseOnSurface = background,
        error = Color(0xFFF2B8B5),
        onError = Color(0xFF601410),
        errorContainer = Color(0xFF8C1D18),
        onErrorContainer = Color(0xFFF9DEDC),
        surfaceContainerLowest = Color(0xFF110E0C),
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceVariant,
        surfaceContainerHighest = surfaceVariant,
        surfaceBright = surfaceVariant,
        surfaceDim = background,
    )

    fun scheme(palette: PaletteId, dark: Boolean): ColorScheme = when (palette) {
        PaletteId.UBUNTU -> if (dark) dark(
            primary = Color(0xFFFFB59B),
            container = Color(0xFF5A2A18),
            onContainer = Color(0xFFFFDBCE),
            accent = Color(0xFFE6BBD8),
            background = Color(0xFF16120F),
            surface = Color(0xFF221D19),
            surfaceVariant = Color(0xFF322925),
            outline = Color(0xFF6C5D55),
        ) else light(
            primary = Color(0xFFD4501C),
            container = Color(0xFFFFDBCE),
            onContainer = Color(0xFF3B0E00),
            accent = Color(0xFF8B4C7E),
            background = Color(0xFFFBF7F4),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFF1E7E0),
            outline = Color(0xFFD8C9BF),
        )

        PaletteId.LAVENDER -> if (dark) dark(
            primary = Color(0xFFCBBDF7),
            container = Color(0xFF3B2F63),
            onContainer = Color(0xFFE7DEFF),
            accent = Color(0xFFC0BDE8),
            background = Color(0xFF131218),
            surface = Color(0xFF1E1C26),
            surfaceVariant = Color(0xFF2C2936),
            outline = Color(0xFF615D75),
        ) else light(
            primary = Color(0xFF6C55C4),
            container = Color(0xFFE7DEFF),
            onContainer = Color(0xFF23085E),
            accent = Color(0xFF7B6EA8),
            background = Color(0xFFF8F6FD),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFEBE7F5),
            outline = Color(0xFFCCC5E2),
        )

        PaletteId.MINT -> if (dark) dark(
            primary = Color(0xFF8DDBB6),
            container = Color(0xFF204E3A),
            onContainer = Color(0xFFB9F5D6),
            accent = Color(0xFF9CD3C6),
            background = Color(0xFF101614),
            surface = Color(0xFF1A2220),
            surfaceVariant = Color(0xFF26312D),
            outline = Color(0xFF55675F),
        ) else light(
            primary = Color(0xFF2F8465),
            container = Color(0xFFC8F2DD),
            onContainer = Color(0xFF00281A),
            accent = Color(0xFF4F8E86),
            background = Color(0xFFF4FAF7),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFE3EFE9),
            outline = Color(0xFFBFD5CB),
        )

        PaletteId.SKY -> if (dark) dark(
            primary = Color(0xFF9CCDF5),
            container = Color(0xFF1E4562),
            onContainer = Color(0xFFCCE5FF),
            accent = Color(0xFFA7C6DE),
            background = Color(0xFF0F1418),
            surface = Color(0xFF191F25),
            surfaceVariant = Color(0xFF252E35),
            outline = Color(0xFF546670),
        ) else light(
            primary = Color(0xFF2C6E9E),
            container = Color(0xFFD1E8FA),
            onContainer = Color(0xFF00243B),
            accent = Color(0xFF4C7F9B),
            background = Color(0xFFF4F8FC),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFE2ECF4),
            outline = Color(0xFFBED2E0),
        )

        PaletteId.ROSE -> if (dark) dark(
            primary = Color(0xFFF6B7C0),
            container = Color(0xFF5E2B36),
            onContainer = Color(0xFFFFD9DF),
            accent = Color(0xFFE5BCA8),
            background = Color(0xFF171214),
            surface = Color(0xFF231B1E),
            surfaceVariant = Color(0xFF32272A),
            outline = Color(0xFF6E5A5F),
        ) else light(
            primary = Color(0xFFBE5570),
            container = Color(0xFFFFDCE3),
            onContainer = Color(0xFF400013),
            accent = Color(0xFFC08268),
            background = Color(0xFFFDF6F7),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFF4E4E7),
            outline = Color(0xFFE0C6CC),
        )
    }
}

/** Extra semantic colors that Material does not define but the app needs. */
object FoksiAccents {
    val success = Color(0xFF3F9D6D)
    val successDark = Color(0xFF8ED9B0)
    val warning = Color(0xFFC98A2E)
    val warningDark = Color(0xFFE9C07A)
    val danger = Color(0xFFC4514F)
    val dangerDark = Color(0xFFF0A9A7)
}
