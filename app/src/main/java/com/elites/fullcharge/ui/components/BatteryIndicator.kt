package com.elites.fullcharge.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elites.fullcharge.ui.theme.*

@Composable
fun BatteryIndicator(
    batteryLevel: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    val isElite = batteryLevel == 100  // 100%만 되면 입장 가능

    val batteryColor by animateColorAsState(
        targetValue = when {
            isElite -> TossBlue
            batteryLevel >= 80 -> TossBlueDark
            batteryLevel >= 50 -> StatusGreen
            batteryLevel >= 20 -> StatusYellow
            else -> StatusRed
        },
        animationSpec = tween(500),
        label = "batteryColor"
    )

    val animatedLevel by animateFloatAsState(
        targetValue = batteryLevel / 100f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "batteryLevel"
    )

    // 충전 중일 때 펄스 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // 100%일 때 회전하는 글로우
    val glowRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glowRotation"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 16f
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)

                // 100%일 때 외부 글로우 링
                if (isElite) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                TossBlue.copy(alpha = 0f),
                                TossBlue.copy(alpha = pulseAlpha),
                                TossBlueLight.copy(alpha = pulseAlpha * 0.8f),
                                TossBlue.copy(alpha = 0f)
                            ),
                            center = center
                        ),
                        radius = radius + 20f,
                        style = Stroke(width = 8f)
                    )
                }

                // 충전 중일 때 외부 펄스 링
                if (isCharging && !isElite) {
                    drawCircle(
                        color = batteryColor.copy(alpha = pulseAlpha * 0.5f),
                        radius = radius * pulseScale + 10f,
                        style = Stroke(width = 4f)
                    )
                }

                // 배경 원
                drawCircle(
                    color = DividerGray,
                    radius = radius,
                    style = Stroke(width = strokeWidth)
                )

                // 배터리 레벨 원호
                if (isElite) {
                    // 100%일 때 그라데이션 원
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                TossBlueDark,
                                TossBlue,
                                TossBlueLight,
                                TossBlue,
                                TossBlueDark
                            ),
                            center = center
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        size = Size(radius * 2, radius * 2),
                        topLeft = Offset(
                            (size.width - radius * 2) / 2,
                            (size.height - radius * 2) / 2
                        )
                    )
                } else {
                    drawArc(
                        color = batteryColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedLevel,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        size = Size(radius * 2, radius * 2),
                        topLeft = Offset(
                            (size.width - radius * 2) / 2,
                            (size.height - radius * 2) / 2
                        )
                    )
                }
            }

            // 배터리 퍼센트 텍스트
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$batteryLevel%",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isElite) TossBlue else TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                // 상태 텍스트
                Text(
                    text = when {
                        isElite -> "입장 가능"
                        isCharging -> "충전 중"
                        else -> "충전 필요"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isElite) TossBlue.copy(alpha = pulseAlpha + 0.2f) else TextSecondary
                )
            }
        }
    }
}

@Composable
fun ChargingProgressBar(
    batteryLevel: Int,
    modifier: Modifier = Modifier
) {
    val animatedLevel by animateFloatAsState(
        targetValue = batteryLevel / 100f,
        animationSpec = tween(500),
        label = "progress"
    )

    // 충전 중 반짝임 효과
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
    ) {
        // 배경
        drawRoundRect(
            color = DividerGray,
            cornerRadius = CornerRadius(4.dp.toPx())
        )

        // 진행 바
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(TossBlueDark, TossBlue, TossBlueLight),
                startX = 0f,
                endX = size.width * animatedLevel
            ),
            size = Size(size.width * animatedLevel, size.height),
            cornerRadius = CornerRadius(4.dp.toPx())
        )

        // 반짝임 효과
        val shimmerWidth = size.width * 0.3f
        val shimmerStart = (size.width * animatedLevel) * shimmerOffset
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.4f),
                    Color.Transparent
                ),
                startX = shimmerStart,
                endX = shimmerStart + shimmerWidth
            ),
            size = Size(size.width * animatedLevel, size.height),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
    }
}
