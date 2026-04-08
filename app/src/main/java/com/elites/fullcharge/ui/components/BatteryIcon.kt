package com.elites.fullcharge.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elites.fullcharge.ui.theme.*

/**
 * v0 스타일 배터리 아이콘 컴포넌트
 * - 스캔라인 효과
 * - 글로우 효과
 * - 충전 중 번개 아이콘
 */

enum class BatteryIconSize {
    SMALL,   // Header용 (48x24)
    MEDIUM,  // 일반용 (80x40)
    LARGE,   // 큰 사이즈 (128x64)
    XLARGE   // 초대형 (192x96)
}

@Composable
fun TacticalBatteryIcon(
    level: Int,
    modifier: Modifier = Modifier,
    charging: Boolean = false,
    size: BatteryIconSize = BatteryIconSize.MEDIUM,
    animated: Boolean = true
) {
    val isFullCharge = level == 100
    val primaryColor = if (isFullCharge) EliteGreen else CrisisRed

    // 크기 설정
    val (width, height) = when (size) {
        BatteryIconSize.SMALL -> 48.dp to 24.dp
        BatteryIconSize.MEDIUM -> 80.dp to 40.dp
        BatteryIconSize.LARGE -> 128.dp to 64.dp
        BatteryIconSize.XLARGE -> 192.dp to 96.dp
    }

    val tipWidth = when (size) {
        BatteryIconSize.SMALL -> 4.dp
        BatteryIconSize.MEDIUM -> 6.dp
        BatteryIconSize.LARGE -> 8.dp
        BatteryIconSize.XLARGE -> 12.dp
    }

    val fontSize = when (size) {
        BatteryIconSize.SMALL -> 10.sp
        BatteryIconSize.MEDIUM -> 14.sp
        BatteryIconSize.LARGE -> 20.sp
        BatteryIconSize.XLARGE -> 28.sp
    }

    // 펄스 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "battery_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (isFullCharge) 0.4f else 0.6f,
        targetValue = if (isFullCharge) 0.8f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // 스캔라인 애니메이션
    val scanlineOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanline"
    )

    val textMeasurer = rememberTextMeasurer()
    val displayAlpha = if (animated) pulseAlpha else 0.8f

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // 글로우 효과
        if (animated) {
            Canvas(
                modifier = Modifier
                    .width(width + 20.dp)
                    .height(height + 20.dp)
            ) {
                drawRoundRect(
                    color = primaryColor.copy(alpha = displayAlpha * 0.3f),
                    cornerRadius = CornerRadius(8f, 8f),
                    size = Size(this.size.width, this.size.height),
                    blendMode = BlendMode.Screen
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 배터리 본체
            Canvas(
                modifier = Modifier
                    .width(width)
                    .height(height)
            ) {
                val batteryWidth = this.size.width
                val batteryHeight = this.size.height
                val borderWidth = 2.dp.toPx()
                val cornerRadius = 4.dp.toPx()

                // 배경 (빈 배터리)
                drawRoundRect(
                    color = BackgroundBlack,
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                    size = Size(batteryWidth, batteryHeight)
                )

                // 테두리
                drawRoundRect(
                    color = primaryColor,
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                    size = Size(batteryWidth, batteryHeight),
                    style = Stroke(width = borderWidth)
                )

                // 충전량 채우기
                val fillWidth = (batteryWidth - borderWidth * 2) * (level / 100f)
                if (level > 0) {
                    drawRoundRect(
                        color = primaryColor,
                        topLeft = Offset(borderWidth, borderWidth),
                        size = Size(fillWidth, batteryHeight - borderWidth * 2),
                        cornerRadius = CornerRadius(cornerRadius / 2, cornerRadius / 2)
                    )
                }

                // 스캔라인 효과
                val scanlineCount = (batteryHeight / 4.dp.toPx()).toInt()
                for (i in 0 until scanlineCount) {
                    val y = i * 4.dp.toPx()
                    drawLine(
                        color = Color.Black.copy(alpha = 0.3f),
                        start = Offset(borderWidth, y),
                        end = Offset(batteryWidth - borderWidth, y),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // 퍼센트 텍스트
                val text = "$level%"
                val textLayoutResult = textMeasurer.measure(
                    text = text,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize
                    )
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    color = if (level > 50) BackgroundBlack else ForegroundWhite,
                    topLeft = Offset(
                        (batteryWidth - textLayoutResult.size.width) / 2,
                        (batteryHeight - textLayoutResult.size.height) / 2
                    )
                )

                // 충전 중 번개 아이콘
                if (charging) {
                    drawLightningIcon(
                        centerX = batteryWidth / 2,
                        centerY = batteryHeight / 2,
                        size = batteryHeight * 0.5f,
                        color = BackgroundBlack.copy(alpha = 0.6f)
                    )
                }
            }

            // 배터리 팁
            Canvas(
                modifier = Modifier
                    .width(tipWidth)
                    .height(height * 0.5f)
            ) {
                drawRoundRect(
                    color = primaryColor,
                    cornerRadius = CornerRadius(2f, 2f),
                    size = Size(this.size.width, this.size.height)
                )
            }
        }
    }
}

private fun DrawScope.drawLightningIcon(
    centerX: Float,
    centerY: Float,
    size: Float,
    color: Color
) {
    val path = Path().apply {
        // 번개 모양
        moveTo(centerX + size * 0.1f, centerY - size * 0.5f)
        lineTo(centerX - size * 0.2f, centerY)
        lineTo(centerX + size * 0.05f, centerY)
        lineTo(centerX - size * 0.1f, centerY + size * 0.5f)
        lineTo(centerX + size * 0.2f, centerY)
        lineTo(centerX - size * 0.05f, centerY)
        close()
    }
    drawPath(path = path, color = color)
}

/**
 * 헤더용 간단한 배터리 표시
 */
@Composable
fun HudBatteryIndicator(
    level: Int,
    modifier: Modifier = Modifier,
    charging: Boolean = false
) {
    val isFullCharge = level == 100
    val primaryColor = if (isFullCharge) EliteGreen else CrisisRed

    val infiniteTransition = rememberInfiniteTransition(label = "hud_battery")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hud_pulse"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TacticalBatteryIcon(
            level = level,
            charging = charging,
            size = BatteryIconSize.SMALL,
            animated = true
        )
    }
}
