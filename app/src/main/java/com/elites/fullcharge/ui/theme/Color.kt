package com.elites.fullcharge.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// 완충 전우회 - Tactical Military Theme (v0 Redesign)
// ============================================================

// === Primary Colors ===
// Elite Green (Neon Green) - 완충 상태, 성공, 활성화
val EliteGreen = Color(0xFF00FF66)
val EliteGreenLight = Color(0xFF66FF99)
val EliteGreenDark = Color(0xFF00CC52)
val EliteGreenMuted = Color(0xFF00FF66).copy(alpha = 0.3f)

// === Destructive Colors ===
// Crisis Red - 위기 상태, 경고, 퇴장
val CrisisRed = Color(0xFFFF4444)
val CrisisRedLight = Color(0xFFFF6B6B)
val CrisisRedDark = Color(0xFFCC3636)
val CrisisRedMuted = Color(0xFFFF4444).copy(alpha = 0.15f)

// === Warning Colors ===
// Warning Amber - 주의, 카운트다운
val WarningAmber = Color(0xFFFFB800)
val WarningAmberLight = Color(0xFFFFCC4D)
val WarningAmberMuted = Color(0xFFFFB800).copy(alpha = 0.1f)

// === Background Colors (Deep Charcoal Black) ===
val BackgroundBlack = Color(0xFF0F0F0F)       // 메인 배경
val CardBlack = Color(0xFF1A1A1A)             // 카드 배경
val SurfaceBlack = Color(0xFF1F1F1F)          // 서피스
val MutedBlack = Color(0xFF2A2A2A)            // 뮤트 배경

// === Foreground / Text Colors ===
val ForegroundWhite = Color(0xFFF0F0F0)       // 메인 텍스트
val ForegroundMuted = Color(0xFF8C8C8C)       // 뮤트 텍스트
val ForegroundDim = Color(0xFF666666)         // 더 어두운 텍스트

// === Border Colors (with green tint) ===
val BorderGreen = Color(0xFF2A3A2A)           // 그린 틴트 테두리
val BorderMuted = Color(0xFF333333)           // 기본 테두리
val BorderSubtle = Color(0xFF252525)          // 미묘한 테두리

// === Accent Colors ===
val AccentGreen = Color(0xFF3D4A3D)           // 밀리터리 그린 틴트
val AccentGreenForeground = EliteGreen

// === Chat Bubble Colors ===
val ChatBubbleSelf = EliteGreen               // 내 메시지
val ChatBubbleOther = CardBlack               // 타인 메시지
val ChatBubbleSystem = MutedBlack             // 시스템 메시지

// === Rank Category Colors ===
val RankEnlisted = Color(0xFFAAAAAA)          // 병 (회색)
val RankNCO = Color(0xFF4D94FF)               // 부사관 (파랑)
val RankOfficer = Color(0xFFFFD700)           // 위관 (금색)
val RankGeneral = Color(0xFFFF6B6B)           // 장성 (빨강)

// === Glow / Shadow Colors ===
val GlowGreen = EliteGreen.copy(alpha = 0.5f)
val GlowRed = CrisisRed.copy(alpha = 0.5f)
val ShadowBlack = Color(0x80000000)

// ============================================================
// 호환성 유지 (기존 코드에서 사용하던 변수명)
// ============================================================
val TossBlue = EliteGreen
val TossBlueLight = EliteGreenLight
val TossBlueDark = EliteGreenDark
val BackgroundWhite = BackgroundBlack
val BackgroundGray = CardBlack
val SurfaceWhite = SurfaceBlack
val TextPrimary = ForegroundWhite
val TextSecondary = ForegroundMuted
val TextTertiary = ForegroundDim
val StatusRed = CrisisRed
val StatusGreen = EliteGreen
val StatusYellow = WarningAmber
val DividerGray = BorderMuted
val BorderGray = BorderMuted
val EliteGold = EliteGreen
val EliteGoldLight = EliteGreenLight
val EliteGoldDark = EliteGreenDark
val EliteWhite = ForegroundWhite
val EliteWhiteLight = ForegroundWhite
val EliteWhiteSurface = CardBlack
val EliteBlack = BackgroundBlack
val EliteBlackLight = CardBlack
val EliteBlackSurface = SurfaceBlack
val UnworthyGray = ForegroundDim
val UnworthyText = ForegroundMuted
val ExileRed = CrisisRed
val GradientGoldStart = EliteGreenLight
val GradientGoldEnd = EliteGreenDark
