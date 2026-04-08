package com.elites.fullcharge.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.elites.fullcharge.ui.theme.*

/**
 * 완충 전우회 앱 로고 컴포넌트
 * v0 설계도의 Shield 아이콘 기반 밀리터리 스타일 로고
 */
@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    animated: Boolean = true,
    showGlow: Boolean = true,
    color: Color = EliteGreen
) {
    // 글로우 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "logo_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.toPx()
            val height = size.toPx()
            val centerX = width / 2
            val centerY = height / 2

            // Glow Effect
            if (showGlow && animated) {
                drawCircle(
                    color = color.copy(alpha = glowAlpha),
                    radius = width * 0.6f,
                    center = Offset(centerX, centerY)
                )
            }

            // Shield Path
            val shieldPath = Path().apply {
                // 상단 중앙에서 시작
                moveTo(centerX, height * 0.08f)
                // 오른쪽 상단 곡선
                cubicTo(
                    width * 0.85f, height * 0.08f,
                    width * 0.92f, height * 0.2f,
                    width * 0.92f, height * 0.35f
                )
                // 오른쪽 아래로
                lineTo(width * 0.92f, height * 0.5f)
                // 하단 뾰족한 부분
                cubicTo(
                    width * 0.92f, height * 0.7f,
                    width * 0.7f, height * 0.85f,
                    centerX, height * 0.95f
                )
                // 왼쪽 하단
                cubicTo(
                    width * 0.3f, height * 0.85f,
                    width * 0.08f, height * 0.7f,
                    width * 0.08f, height * 0.5f
                )
                // 왼쪽 위로
                lineTo(width * 0.08f, height * 0.35f)
                // 왼쪽 상단 곡선
                cubicTo(
                    width * 0.08f, height * 0.2f,
                    width * 0.15f, height * 0.08f,
                    centerX, height * 0.08f
                )
                close()
            }

            // Shield Fill
            drawPath(
                path = shieldPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.2f),
                        color.copy(alpha = 0.05f)
                    )
                )
            )

            // Shield Border
            drawPath(
                path = shieldPath,
                color = color,
                style = Stroke(width = width * 0.04f)
            )

            // 100% 표시를 위한 내부 디자인
            // 번개 아이콘 (충전 심볼)
            val lightningPath = Path().apply {
                moveTo(centerX + width * 0.08f, height * 0.25f)
                lineTo(centerX - width * 0.12f, height * 0.5f)
                lineTo(centerX, height * 0.5f)
                lineTo(centerX - width * 0.08f, height * 0.75f)
                lineTo(centerX + width * 0.12f, height * 0.5f)
                lineTo(centerX, height * 0.5f)
                close()
            }

            drawPath(
                path = lightningPath,
                color = color
            )
        }
    }
}

/**
 * 헤더용 로고 + 텍스트 조합
 */
@Composable
fun AppLogoWithText(
    modifier: Modifier = Modifier,
    logoSize: Dp = 20.dp,
    showEnglish: Boolean = true,
    color: Color = EliteGreen
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        AppLogo(
            size = logoSize,
            animated = false,
            showGlow = false,
            color = color
        )
        Spacer(modifier = Modifier.width(8.dp))
        if (showEnglish) {
            Text(
                text = "검문소 | CHECKPOINT",
                style = MonoTypography.tracking,
                color = ForegroundMuted
            )
        } else {
            Text(
                text = "완충 전우회",
                style = MaterialTheme.typography.titleSmall,
                color = ForegroundWhite
            )
        }
    }
}

/**
 * 푸터용 로고
 */
@Composable
fun AppLogoFooter(
    modifier: Modifier = Modifier,
    isActive: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // 상태 표시 점
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(
                color = if (isActive) EliteGreen else CrisisRed
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "완충 전우회 | THE 100% ELITES",
            style = MonoTypography.hudSmall,
            color = ForegroundMuted
        )
    }
}

/**
 * Security Badge (잠금/해제 상태 표시)
 * v0: w-32 h-32 (128px), border-4, 아이콘 w-16 h-16 (64px)
 */
@Composable
fun SecurityBadge(
    modifier: Modifier = Modifier,
    isUnlocked: Boolean,
    size: Dp = 140.dp,  // v0보다 약간 크게 (128 + 글로우 영역)
    animated: Boolean = true
) {
    val color = if (isUnlocked) EliteGreen else CrisisRed

    // 글로우 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "badge_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // 펄스 애니메이션 (잠금 해제 시)
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Blur Glow Background (v0: absolute -inset-8 rounded-full blur-3xl opacity-30)
        if (animated) {
            Canvas(modifier = Modifier.size(size + 32.dp)) {
                drawCircle(
                    color = color.copy(alpha = glowAlpha),
                    radius = this.size.minDimension / 2
                )
            }
        }

        // Main Badge Circle (v0: w-32 h-32 = 128px)
        Canvas(modifier = Modifier.size(128.dp)) {
            val circleRadius = this.size.minDimension / 2
            val centerX = this.size.width / 2
            val centerY = this.size.height / 2

            // Background circle (v0: bg-primary/10)
            drawCircle(
                color = color.copy(alpha = 0.1f),
                radius = circleRadius
            )

            // Border circle (v0: border-4 = 4dp)
            drawCircle(
                color = color,
                radius = circleRadius - 2.dp.toPx(),
                style = Stroke(width = 4.dp.toPx())
            )

            // Lock/Unlock Icon (v0: w-16 h-16 = 64px, 배지의 50%)
            val iconSize = circleRadius * 0.9f  // 아이콘 크기를 더 크게

            if (isUnlocked) {
                drawUnlockIcon(centerX, centerY, iconSize, color.copy(alpha = if (animated) pulseAlpha else 1f))
            } else {
                drawLockIcon(centerX, centerY, iconSize, color)
            }
        }
    }
}

private fun DrawScope.drawLockIcon(
    centerX: Float,
    centerY: Float,
    iconSize: Float,
    color: Color
) {
    val lockPath = Path().apply {
        // 자물쇠 몸체
        val bodyWidth = iconSize * 0.7f
        val bodyHeight = iconSize * 0.5f
        val bodyTop = centerY
        val bodyLeft = centerX - bodyWidth / 2

        // 둥근 사각형 몸체
        moveTo(bodyLeft + 4, bodyTop)
        lineTo(bodyLeft + bodyWidth - 4, bodyTop)
        cubicTo(
            bodyLeft + bodyWidth, bodyTop,
            bodyLeft + bodyWidth, bodyTop + 4,
            bodyLeft + bodyWidth, bodyTop + 4
        )
        lineTo(bodyLeft + bodyWidth, bodyTop + bodyHeight - 4)
        cubicTo(
            bodyLeft + bodyWidth, bodyTop + bodyHeight,
            bodyLeft + bodyWidth - 4, bodyTop + bodyHeight,
            bodyLeft + bodyWidth - 4, bodyTop + bodyHeight
        )
        lineTo(bodyLeft + 4, bodyTop + bodyHeight)
        cubicTo(
            bodyLeft, bodyTop + bodyHeight,
            bodyLeft, bodyTop + bodyHeight - 4,
            bodyLeft, bodyTop + bodyHeight - 4
        )
        lineTo(bodyLeft, bodyTop + 4)
        cubicTo(
            bodyLeft, bodyTop,
            bodyLeft + 4, bodyTop,
            bodyLeft + 4, bodyTop
        )
        close()
    }

    drawPath(path = lockPath, color = color)

    // Shackle (잠금쇠 고리)
    val shacklePath = Path().apply {
        val shackleWidth = iconSize * 0.4f
        val shackleHeight = iconSize * 0.35f
        val shackleLeft = centerX - shackleWidth / 2
        val shackleTop = centerY - shackleHeight

        moveTo(shackleLeft, centerY)
        lineTo(shackleLeft, shackleTop + 8)
        cubicTo(
            shackleLeft, shackleTop,
            centerX, shackleTop,
            centerX, shackleTop
        )
        cubicTo(
            centerX, shackleTop,
            shackleLeft + shackleWidth, shackleTop,
            shackleLeft + shackleWidth, shackleTop + 8
        )
        lineTo(shackleLeft + shackleWidth, centerY)
    }

    drawPath(
        path = shacklePath,
        color = color,
        style = Stroke(width = iconSize * 0.12f)
    )
}

private fun DrawScope.drawUnlockIcon(
    centerX: Float,
    centerY: Float,
    iconSize: Float,
    color: Color
) {
    val lockPath = Path().apply {
        // 자물쇠 몸체
        val bodyWidth = iconSize * 0.7f
        val bodyHeight = iconSize * 0.5f
        val bodyTop = centerY
        val bodyLeft = centerX - bodyWidth / 2

        moveTo(bodyLeft + 4, bodyTop)
        lineTo(bodyLeft + bodyWidth - 4, bodyTop)
        cubicTo(
            bodyLeft + bodyWidth, bodyTop,
            bodyLeft + bodyWidth, bodyTop + 4,
            bodyLeft + bodyWidth, bodyTop + 4
        )
        lineTo(bodyLeft + bodyWidth, bodyTop + bodyHeight - 4)
        cubicTo(
            bodyLeft + bodyWidth, bodyTop + bodyHeight,
            bodyLeft + bodyWidth - 4, bodyTop + bodyHeight,
            bodyLeft + bodyWidth - 4, bodyTop + bodyHeight
        )
        lineTo(bodyLeft + 4, bodyTop + bodyHeight)
        cubicTo(
            bodyLeft, bodyTop + bodyHeight,
            bodyLeft, bodyTop + bodyHeight - 4,
            bodyLeft, bodyTop + bodyHeight - 4
        )
        lineTo(bodyLeft, bodyTop + 4)
        cubicTo(
            bodyLeft, bodyTop,
            bodyLeft + 4, bodyTop,
            bodyLeft + 4, bodyTop
        )
        close()
    }

    drawPath(path = lockPath, color = color)

    // Open Shackle (열린 잠금쇠 고리)
    val shacklePath = Path().apply {
        val shackleWidth = iconSize * 0.4f
        val shackleHeight = iconSize * 0.35f
        val shackleLeft = centerX - shackleWidth / 2
        val shackleTop = centerY - shackleHeight

        // 왼쪽만 연결, 오른쪽은 열림
        moveTo(shackleLeft, centerY)
        lineTo(shackleLeft, shackleTop + 8)
        cubicTo(
            shackleLeft, shackleTop,
            centerX, shackleTop,
            centerX, shackleTop
        )
        cubicTo(
            centerX, shackleTop,
            shackleLeft + shackleWidth, shackleTop,
            shackleLeft + shackleWidth, shackleTop + 8
        )
        // 오른쪽은 위로 열림
        lineTo(shackleLeft + shackleWidth, shackleTop - 4)
    }

    drawPath(
        path = shacklePath,
        color = color,
        style = Stroke(width = iconSize * 0.12f)
    )
}
