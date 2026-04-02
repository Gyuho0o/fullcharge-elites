package com.elites.fullcharge.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TossLightColorScheme = lightColorScheme(
    primary = TossBlue,
    onPrimary = Color.White,
    primaryContainer = TossBlueLight,
    onPrimaryContainer = Color.White,

    secondary = TossBlueDark,
    onSecondary = Color.White,
    secondaryContainer = BackgroundGray,
    onSecondaryContainer = TextPrimary,

    tertiary = TossBlueDark,
    onTertiary = Color.White,

    background = BackgroundWhite,
    onBackground = TextPrimary,

    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundGray,
    onSurfaceVariant = TextSecondary,

    error = StatusRed,
    onError = Color.White,

    outline = BorderGray,
    outlineVariant = DividerGray
)

@Composable
fun ElitesTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TossLightColorScheme,
        typography = Typography,
        content = content
    )
}
