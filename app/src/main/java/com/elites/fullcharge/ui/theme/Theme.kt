package com.elites.fullcharge.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ============================================================
// 완충 전우회 - Tactical Military Dark Theme
// ============================================================

private val TacticalDarkColorScheme = darkColorScheme(
    // Primary - Elite Green (네온 그린)
    primary = EliteGreen,
    onPrimary = BackgroundBlack,
    primaryContainer = EliteGreenDark,
    onPrimaryContainer = ForegroundWhite,

    // Secondary - Muted Green
    secondary = AccentGreen,
    onSecondary = ForegroundWhite,
    secondaryContainer = MutedBlack,
    onSecondaryContainer = ForegroundWhite,

    // Tertiary - Warning Amber
    tertiary = WarningAmber,
    onTertiary = BackgroundBlack,
    tertiaryContainer = WarningAmberMuted,
    onTertiaryContainer = WarningAmber,

    // Background - Deep Charcoal Black
    background = BackgroundBlack,
    onBackground = ForegroundWhite,

    // Surface - Slightly lighter black
    surface = SurfaceBlack,
    onSurface = ForegroundWhite,
    surfaceVariant = CardBlack,
    onSurfaceVariant = ForegroundMuted,

    // Error/Destructive - Crisis Red
    error = CrisisRed,
    onError = ForegroundWhite,
    errorContainer = CrisisRedMuted,
    onErrorContainer = CrisisRed,

    // Outline/Border
    outline = BorderMuted,
    outlineVariant = BorderSubtle,

    // Inverse
    inverseSurface = ForegroundWhite,
    inverseOnSurface = BackgroundBlack,
    inversePrimary = EliteGreenDark,

    // Scrim
    scrim = ShadowBlack,

    // Surface Tint
    surfaceTint = EliteGreen
)

@Composable
fun ElitesTheme(
    darkTheme: Boolean = true, // 항상 다크 테마 사용
    content: @Composable () -> Unit
) {
    val colorScheme = TacticalDarkColorScheme

    // 시스템 바 색상 설정
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BackgroundBlack.toArgb()
            window.navigationBarColor = BackgroundBlack.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// ============================================================
// 커스텀 테마 확장 (MaterialTheme에 없는 색상들)
// ============================================================

object ElitesColors {
    // Primary States
    val eliteGreen = EliteGreen
    val eliteGreenLight = EliteGreenLight
    val eliteGreenDark = EliteGreenDark
    val eliteGreenMuted = EliteGreenMuted

    // Crisis States
    val crisisRed = CrisisRed
    val crisisRedLight = CrisisRedLight
    val crisisRedMuted = CrisisRedMuted

    // Warning States
    val warningAmber = WarningAmber
    val warningAmberLight = WarningAmberLight

    // Backgrounds
    val background = BackgroundBlack
    val card = CardBlack
    val surface = SurfaceBlack
    val muted = MutedBlack

    // Text
    val foreground = ForegroundWhite
    val foregroundMuted = ForegroundMuted
    val foregroundDim = ForegroundDim

    // Borders
    val border = BorderMuted
    val borderGreen = BorderGreen
    val borderSubtle = BorderSubtle

    // Glow Effects
    val glowGreen = GlowGreen
    val glowRed = GlowRed

    // Rank Colors
    val rankEnlisted = RankEnlisted
    val rankNCO = RankNCO
    val rankOfficer = RankOfficer
    val rankGeneral = RankGeneral
}
