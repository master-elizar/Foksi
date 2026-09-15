package com.foksi.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.foksi.app.domain.model.AppSettings
import com.foksi.app.domain.model.ThemeMode

val FoksiShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
)

data class FoksiExtraColors(
    val success: Color,
    val warning: Color,
    val danger: Color,
    val isDark: Boolean,
)

val LocalFoksiColors = staticCompositionLocalOf {
    FoksiExtraColors(FoksiAccents.success, FoksiAccents.warning, FoksiAccents.danger, false)
}

val LocalAppSettings = staticCompositionLocalOf { AppSettings() }

@Composable
fun FoksiTheme(
    settings: AppSettings = AppSettings(),
    content: @Composable () -> Unit,
) {
    val dark = when (settings.theme) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val scheme = FoksiPalettes.scheme(settings.palette, dark)
    val extras = if (dark) {
        FoksiExtraColors(
            FoksiAccents.successDark, FoksiAccents.warningDark, FoksiAccents.dangerDark, true
        )
    } else {
        FoksiExtraColors(FoksiAccents.success, FoksiAccents.warning, FoksiAccents.danger, false)
    }

    CompositionLocalProvider(
        LocalFoksiColors provides extras,
        LocalAppSettings provides settings,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = foksiTypography(settings.textSize.scale),
            shapes = FoksiShapes,
            content = content,
        )
    }
}
