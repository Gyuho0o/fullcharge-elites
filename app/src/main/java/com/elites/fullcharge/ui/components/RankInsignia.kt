package com.elites.fullcharge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.elites.fullcharge.data.EliteRank
import kotlin.math.cos
import kotlin.math.sin

/**
 * 실제 대한민국 군 계급장 UI 컴포넌트
 */

// 색상 정의
private val EnlistedBarColor = Color(0xFF1A1A1A)  // 병 - 검은색 가로줄
private val EnlistedBgColor = Color(0xFF4A5D23)   // 병 - 올리브색 배경
private val NcoGoldColor = Color(0xFFD4AF37)      // 부사관 - 금색
private val OfficerBlueColor = Color(0xFF7BA3D0)  // 장교 - 은청색

@Composable
fun RankInsignia(
    rank: EliteRank,
    size: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    val sizePx = with(LocalDensity.current) { size.toPx() }

    Canvas(modifier = modifier.size(size)) {
        when (rank) {
            // ===== 훈련병: 빈 원 =====
            EliteRank.TRAINEE -> {
                drawCircle(
                    color = EnlistedBgColor,
                    radius = sizePx * 0.4f,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // ===== 병: 가로줄 (1~4개) =====
            EliteRank.PRIVATE_SECOND, EliteRank.PRIVATE_FIRST,
            EliteRank.CORPORAL, EliteRank.SERGEANT -> {
                val barCount = when (rank) {
                    EliteRank.PRIVATE_SECOND -> 1
                    EliteRank.PRIVATE_FIRST -> 2
                    EliteRank.CORPORAL -> 3
                    EliteRank.SERGEANT -> 4
                    else -> 1
                }
                val barHeight = sizePx * 0.12f
                val barSpacing = sizePx * 0.18f
                val totalHeight = barCount * barHeight + (barCount - 1) * barSpacing
                val startY = (sizePx - totalHeight) / 2

                // 배경 (올리브색 사각형)
                drawRoundRect(
                    color = EnlistedBgColor,
                    topLeft = Offset(sizePx * 0.1f, startY - barSpacing * 0.5f),
                    size = Size(sizePx * 0.8f, totalHeight + barSpacing),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )

                // 검은색 가로줄
                for (i in 0 until barCount) {
                    val y = startY + i * (barHeight + barSpacing)
                    drawRoundRect(
                        color = EnlistedBarColor,
                        topLeft = Offset(sizePx * 0.15f, y),
                        size = Size(sizePx * 0.7f, barHeight),
                        cornerRadius = CornerRadius(1.dp.toPx())
                    )
                }
            }

            // ===== 부사관: 금색 갈매기 (∨) =====
            EliteRank.STAFF_SERGEANT, EliteRank.SERGEANT_FIRST,
            EliteRank.MASTER_SERGEANT, EliteRank.SERGEANT_MAJOR -> {
                val chevronCount = when (rank) {
                    EliteRank.STAFF_SERGEANT -> 1
                    EliteRank.SERGEANT_FIRST -> 2
                    EliteRank.MASTER_SERGEANT -> 3
                    EliteRank.SERGEANT_MAJOR -> 4
                    else -> 1
                }
                val chevronHeight = sizePx * 0.15f
                val chevronSpacing = sizePx * 0.12f
                val strokeWidth = 2.5.dp.toPx()

                // 원사는 별 추가
                if (rank == EliteRank.SERGEANT_MAJOR) {
                    drawStar(
                        center = Offset(sizePx / 2, sizePx * 0.15f),
                        radius = sizePx * 0.12f,
                        color = NcoGoldColor
                    )
                }

                val startY = if (rank == EliteRank.SERGEANT_MAJOR) sizePx * 0.3f else sizePx * 0.2f

                for (i in 0 until chevronCount) {
                    val y = startY + i * chevronSpacing
                    val path = Path().apply {
                        moveTo(sizePx * 0.2f, y)
                        lineTo(sizePx / 2, y + chevronHeight)
                        lineTo(sizePx * 0.8f, y)
                    }
                    drawPath(
                        path = path,
                        color = NcoGoldColor,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            // ===== 위관장교: 은청색 다이아몬드 =====
            EliteRank.SECOND_LIEUTENANT, EliteRank.FIRST_LIEUTENANT, EliteRank.CAPTAIN -> {
                val diamondCount = when (rank) {
                    EliteRank.SECOND_LIEUTENANT -> 1
                    EliteRank.FIRST_LIEUTENANT -> 2
                    EliteRank.CAPTAIN -> 3
                    else -> 1
                }
                val diamondSize = sizePx * 0.25f
                val spacing = sizePx * 0.22f
                val totalWidth = diamondCount * diamondSize + (diamondCount - 1) * (spacing - diamondSize * 0.3f)
                val startX = (sizePx - totalWidth) / 2

                for (i in 0 until diamondCount) {
                    val x = startX + i * spacing
                    val y = sizePx / 2
                    drawDiamond(
                        center = Offset(x + diamondSize / 2, y),
                        size = diamondSize,
                        color = OfficerBlueColor
                    )
                }
            }

            // ===== 영관장교: 은청색 무궁화 =====
            EliteRank.MAJOR, EliteRank.LIEUTENANT_COLONEL, EliteRank.COLONEL -> {
                val flowerCount = when (rank) {
                    EliteRank.MAJOR -> 1
                    EliteRank.LIEUTENANT_COLONEL -> 2
                    EliteRank.COLONEL -> 3
                    else -> 1
                }
                val flowerSize = sizePx * 0.28f
                val spacing = sizePx * 0.26f
                val totalWidth = flowerCount * flowerSize + (flowerCount - 1) * (spacing - flowerSize * 0.2f)
                val startX = (sizePx - totalWidth) / 2

                for (i in 0 until flowerCount) {
                    val x = startX + i * spacing
                    val y = sizePx / 2
                    drawFlower(
                        center = Offset(x + flowerSize / 2, y),
                        radius = flowerSize / 2,
                        color = OfficerBlueColor
                    )
                }
            }

            // ===== 장성: 은청색 별 =====
            EliteRank.BRIGADIER_GENERAL, EliteRank.MAJOR_GENERAL,
            EliteRank.LIEUTENANT_GENERAL, EliteRank.GENERAL -> {
                val starCount = when (rank) {
                    EliteRank.BRIGADIER_GENERAL -> 1
                    EliteRank.MAJOR_GENERAL -> 2
                    EliteRank.LIEUTENANT_GENERAL -> 3
                    EliteRank.GENERAL -> 4
                    else -> 1
                }
                val starSize = sizePx * 0.25f
                val spacing = sizePx * 0.22f
                val totalWidth = starCount * starSize + (starCount - 1) * (spacing - starSize * 0.3f)
                val startX = (sizePx - totalWidth) / 2

                for (i in 0 until starCount) {
                    val x = startX + i * spacing
                    val y = sizePx / 2
                    drawStar(
                        center = Offset(x + starSize / 2, y),
                        radius = starSize / 2,
                        color = OfficerBlueColor
                    )
                }
            }
        }
    }
}

// 별 그리기
private fun DrawScope.drawStar(
    center: Offset,
    radius: Float,
    color: Color
) {
    val path = Path()
    val innerRadius = radius * 0.4f

    for (i in 0 until 10) {
        val r = if (i % 2 == 0) radius else innerRadius
        val angle = Math.PI / 2 + i * Math.PI / 5
        val x = center.x + (r * cos(angle)).toFloat()
        val y = center.y - (r * sin(angle)).toFloat()

        if (i == 0) path.moveTo(x, y)
        else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

// 다이아몬드 그리기
private fun DrawScope.drawDiamond(
    center: Offset,
    size: Float,
    color: Color
) {
    val halfSize = size / 2
    val path = Path().apply {
        moveTo(center.x, center.y - halfSize)  // 상단
        lineTo(center.x + halfSize * 0.6f, center.y)  // 우측
        lineTo(center.x, center.y + halfSize)  // 하단
        lineTo(center.x - halfSize * 0.6f, center.y)  // 좌측
        close()
    }
    drawPath(path, color)
}

// 무궁화 그리기 (간소화된 버전)
private fun DrawScope.drawFlower(
    center: Offset,
    radius: Float,
    color: Color
) {
    // 5개의 꽃잎
    for (i in 0 until 5) {
        val angle = Math.PI / 2 + i * 2 * Math.PI / 5
        val petalX = center.x + (radius * 0.5f * cos(angle)).toFloat()
        val petalY = center.y - (radius * 0.5f * sin(angle)).toFloat()

        drawCircle(
            color = color,
            radius = radius * 0.4f,
            center = Offset(petalX, petalY)
        )
    }
    // 중앙
    drawCircle(
        color = color.copy(alpha = 0.8f),
        radius = radius * 0.3f,
        center = center
    )
}
