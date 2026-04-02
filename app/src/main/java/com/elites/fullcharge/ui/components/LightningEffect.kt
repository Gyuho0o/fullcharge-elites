package com.elites.fullcharge.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.elites.fullcharge.ui.theme.TossBlue
import com.elites.fullcharge.ui.theme.TossBlueLight
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 번개 효과 - 화면에 번개가 파파박 튀는 효과
 */
@Composable
fun LightningBurst(
    modifier: Modifier = Modifier,
    trigger: Boolean = false,
    continuous: Boolean = false,
    color: Color = TossBlueLight
) {
    var lightnings by remember { mutableStateOf<List<LightningBolt>>(emptyList()) }
    var flashAlpha by remember { mutableFloatStateOf(0f) }

    // 트리거되면 번개 생성
    LaunchedEffect(trigger) {
        if (trigger) {
            repeat(3) {
                lightnings = lightnings + generateLightningBolt()
                flashAlpha = 0.3f
                delay(50)
                flashAlpha = 0f
                delay(30)
            }
            delay(200)
            lightnings = emptyList()
        }
    }

    // 연속 모드
    LaunchedEffect(continuous) {
        if (continuous) {
            while (true) {
                delay(Random.nextLong(2000, 5000))
                repeat(Random.nextInt(2, 4)) {
                    lightnings = lightnings + generateLightningBolt()
                    flashAlpha = 0.2f
                    delay(60)
                    flashAlpha = 0f
                    delay(40)
                }
                delay(150)
                lightnings = emptyList()
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        // 플래시 효과
        if (flashAlpha > 0) {
            drawRect(color = Color.White.copy(alpha = flashAlpha))
        }

        // 번개 그리기
        lightnings.forEach { bolt ->
            drawLightningBolt(bolt, color)
        }
    }
}

/**
 * 메시지 전송 시 번개 효과
 */
@Composable
fun MessageLightningEffect(
    trigger: Int, // 메시지 수가 변경될 때마다 트리거
    modifier: Modifier = Modifier
) {
    var bolts by remember { mutableStateOf<List<SmallBolt>>(emptyList()) }

    LaunchedEffect(trigger) {
        if (trigger > 0) {
            // 여러 개의 작은 번개 생성
            bolts = List(5) { generateSmallBolt() }
            delay(150)
            bolts = emptyList()
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        bolts.forEach { bolt ->
            drawSmallBolt(bolt)
        }
    }
}

/**
 * 입장 화면용 배경 번개
 */
@Composable
fun BackgroundLightning(
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    if (!enabled) return

    var bolts by remember { mutableStateOf<List<LightningBolt>>(emptyList()) }
    var flashAlpha by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(enabled) {
        while (enabled) {
            delay(Random.nextLong(3000, 6000))

            // 번개 시퀀스
            repeat(Random.nextInt(2, 4)) {
                bolts = List(Random.nextInt(1, 3)) { generateLightningBolt() }
                flashAlpha = Random.nextFloat() * 0.15f + 0.05f
                delay(80)
                flashAlpha = 0f
                delay(50)
            }
            bolts = emptyList()
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        // 미세한 플래시
        if (flashAlpha > 0) {
            drawRect(color = TossBlueLight.copy(alpha = flashAlpha))
        }

        // 번개
        bolts.forEach { bolt ->
            drawLightningBolt(bolt, TossBlueLight.copy(alpha = 0.6f))
        }
    }
}

// 데이터 클래스들
private data class LightningBolt(
    val points: List<Offset>,
    val branches: List<List<Offset>>
)

private data class SmallBolt(
    val startX: Float,
    val startY: Float,
    val points: List<Offset>,
    val alpha: Float
)

// 번개 생성 함수들
private fun generateLightningBolt(): LightningBolt {
    val startX = Random.nextFloat() * 0.6f + 0.2f
    val points = mutableListOf<Offset>()
    var x = startX
    var y = 0f

    points.add(Offset(x, y))

    while (y < 1f) {
        x += (Random.nextFloat() - 0.5f) * 0.15f
        y += Random.nextFloat() * 0.12f + 0.05f
        points.add(Offset(x.coerceIn(0.05f, 0.95f), y.coerceIn(0f, 1f)))
    }

    // 가지 생성
    val branches = mutableListOf<List<Offset>>()
    points.forEachIndexed { index, point ->
        if (index > 0 && index < points.size - 2 && Random.nextFloat() > 0.6f) {
            val branch = mutableListOf<Offset>()
            var bx = point.x
            var by = point.y
            val direction = if (Random.nextBoolean()) 1 else -1

            repeat(Random.nextInt(2, 5)) {
                bx += direction * Random.nextFloat() * 0.08f
                by += Random.nextFloat() * 0.06f
                branch.add(Offset(bx.coerceIn(0f, 1f), by.coerceIn(0f, 1f)))
            }
            branches.add(branch)
        }
    }

    return LightningBolt(points, branches)
}

private fun generateSmallBolt(): SmallBolt {
    val startX = Random.nextFloat()
    val startY = Random.nextFloat() * 0.3f + 0.35f
    val points = mutableListOf<Offset>()

    var x = 0f
    var y = 0f
    val angle = Random.nextFloat() * Math.PI.toFloat() * 2

    repeat(Random.nextInt(3, 6)) {
        x += cos(angle + (Random.nextFloat() - 0.5f)) * 0.03f
        y += sin(angle + (Random.nextFloat() - 0.5f)) * 0.03f
        points.add(Offset(x, y))
    }

    return SmallBolt(startX, startY, points, Random.nextFloat() * 0.5f + 0.5f)
}

// 그리기 함수들
private fun DrawScope.drawLightningBolt(bolt: LightningBolt, color: Color) {
    val scaledPoints = bolt.points.map {
        Offset(it.x * size.width, it.y * size.height)
    }

    // 외부 글로우
    for (i in 0 until scaledPoints.size - 1) {
        drawLine(
            color = color.copy(alpha = 0.3f),
            start = scaledPoints[i],
            end = scaledPoints[i + 1],
            strokeWidth = 12f,
            cap = StrokeCap.Round
        )
    }

    // 메인 번개
    for (i in 0 until scaledPoints.size - 1) {
        drawLine(
            color = color.copy(alpha = 0.8f),
            start = scaledPoints[i],
            end = scaledPoints[i + 1],
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
    }

    // 밝은 중심
    for (i in 0 until scaledPoints.size - 1) {
        drawLine(
            color = Color.White.copy(alpha = 0.9f),
            start = scaledPoints[i],
            end = scaledPoints[i + 1],
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
    }

    // 가지들
    bolt.branches.forEach { branch ->
        val branchStart = scaledPoints.getOrNull(
            bolt.points.indexOfFirst { it.x == branch.firstOrNull()?.x }
        ) ?: return@forEach

        var prevPoint = branchStart
        branch.forEach { point ->
            val scaled = Offset(
                branchStart.x + point.x * size.width * 0.3f,
                branchStart.y + point.y * size.height * 0.3f
            )
            drawLine(
                color = color.copy(alpha = 0.5f),
                start = prevPoint,
                end = scaled,
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )
            prevPoint = scaled
        }
    }
}

private fun DrawScope.drawSmallBolt(bolt: SmallBolt) {
    val startOffset = Offset(bolt.startX * size.width, bolt.startY * size.height)

    var prevPoint = startOffset
    bolt.points.forEach { point ->
        val scaled = Offset(
            startOffset.x + point.x * size.width,
            startOffset.y + point.y * size.height
        )

        // 글로우
        drawLine(
            color = TossBlueLight.copy(alpha = bolt.alpha * 0.4f),
            start = prevPoint,
            end = scaled,
            strokeWidth = 8f,
            cap = StrokeCap.Round
        )

        // 메인
        drawLine(
            color = TossBlueLight.copy(alpha = bolt.alpha * 0.8f),
            start = prevPoint,
            end = scaled,
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )

        // 밝은 중심
        drawLine(
            color = Color.White.copy(alpha = bolt.alpha),
            start = prevPoint,
            end = scaled,
            strokeWidth = 1.5f,
            cap = StrokeCap.Round
        )

        prevPoint = scaled
    }
}
