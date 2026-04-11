package com.elites.fullcharge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.elites.fullcharge.ui.theme.EliteGreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ============================================================
// 타이핑 스파크 이펙트 - 모든 계급 상시 활성화
// ============================================================

/**
 * 타이핑 스파크 이펙트
 * 입력 시 커서 주변에 작은 스파크가 튀는 효과
 *
 * @param isTyping 현재 입력 중 여부
 * @param textLength 입력된 텍스트 길이 (강도 조절용)
 * @param modifier Modifier
 */
@Composable
fun TypingSparkEffect(
    isTyping: Boolean,
    textLength: Int,
    modifier: Modifier = Modifier
) {
    // 텍스트 길이에 따라 스파크 강도 조절 (0.5 ~ 1.0)
    val intensity = (0.5f + (textLength.coerceIn(0, 50) / 50f) * 0.5f)

    // 스파크 파티클 목록
    var particles by remember { mutableStateOf(listOf<SparkParticle>()) }

    // 스파크 생성
    LaunchedEffect(isTyping, textLength) {
        if (isTyping && textLength > 0) {
            while (true) {
                // 텍스트 길이에 비례하여 스파크 생성 빈도 조절
                val spawnCount = (2 + (intensity * 3)).toInt()
                val newParticles = (0 until spawnCount).map {
                    SparkParticle(
                        x = Random.nextFloat() * 80f - 40f,
                        y = Random.nextFloat() * 60f - 30f,
                        angle = Random.nextFloat() * 360f,
                        speed = 3f + Random.nextFloat() * 4f,
                        size = 4f + Random.nextFloat() * 5f * intensity,
                        alpha = 1f,
                        life = 0.6f + Random.nextFloat() * 0.4f,
                        color = if (Random.nextBoolean()) EliteGreen else Color(0xFF00BFFF)
                    )
                }
                particles = (particles + newParticles).takeLast(25)
                delay((80 / intensity).toLong())
            }
        } else {
            particles = emptyList()
        }
    }

    // 파티클 애니메이션
    LaunchedEffect(particles.isNotEmpty()) {
        while (particles.isNotEmpty()) {
            delay(16) // ~60fps
            particles = particles.mapNotNull { particle ->
                val newLife = particle.life - 0.04f
                if (newLife <= 0) null
                else particle.copy(
                    x = particle.x + cos(particle.angle * PI / 180).toFloat() * particle.speed,
                    y = particle.y + sin(particle.angle * PI / 180).toFloat() * particle.speed - 1f,
                    alpha = (particle.alpha * 0.94f).coerceAtLeast(0.3f),
                    life = newLife
                )
            }
        }
    }

    // 항상 Canvas 렌더링 (파티클이 있을 때만 그리기)
    Canvas(
        modifier = modifier.size(100.dp, 70.dp)
    ) {
        particles.forEach { particle ->
            val centerX = size.width / 2
            val centerY = size.height / 2

            // 글로우 효과 (더 큰 반투명 원)
            drawCircle(
                color = particle.color.copy(alpha = particle.alpha * 0.3f),
                radius = particle.size * 2.5f,
                center = Offset(centerX + particle.x, centerY + particle.y)
            )

            // 스파크 본체
            drawCircle(
                color = particle.color.copy(alpha = particle.alpha),
                radius = particle.size,
                center = Offset(centerX + particle.x, centerY + particle.y)
            )

            // 스파크 꼬리 (더 두꺼운 선)
            val tailLength = particle.speed * 2.5f
            drawLine(
                color = particle.color.copy(alpha = particle.alpha * 0.7f),
                start = Offset(centerX + particle.x, centerY + particle.y),
                end = Offset(
                    centerX + particle.x - cos(particle.angle * PI / 180).toFloat() * tailLength,
                    centerY + particle.y - sin(particle.angle * PI / 180).toFloat() * tailLength
                ),
                strokeWidth = 2.5f
            )
        }
    }
}

private data class SparkParticle(
    val x: Float,
    val y: Float,
    val angle: Float,
    val speed: Float,
    val size: Float,
    val alpha: Float,
    val life: Float,
    val color: Color
)

// ============================================================
// 전체 화면 타이핑 번개 이펙트 - 부사관/장교 전용
// ============================================================

/**
 * 전체 화면 타이핑 번개 이펙트
 * 타이핑 시 화면 전체에 번개가 치는 효과
 *
 * @param isTyping 현재 타이핑 중 여부
 * @param lightningColor 번개 색상 (부사관: 초록, 장교: 파랑)
 * @param modifier Modifier
 */
@Composable
fun FullScreenTypingLightning(
    isTyping: Boolean,
    lightningColor: Color,
    modifier: Modifier = Modifier
) {
    var lightningBolts by remember { mutableStateOf(listOf<TypingLightningBolt>()) }
    var flashAlpha by remember { mutableStateOf(0f) }
    var lastTriggerTime by remember { mutableStateOf(0L) }

    // 타이핑 시 번개 생성
    LaunchedEffect(isTyping) {
        if (isTyping) {
            try {
                while (true) {
                    val currentTime = System.currentTimeMillis()
                    // 최소 200ms 간격으로 번개 생성
                    if (currentTime - lastTriggerTime >= 200) {
                        lastTriggerTime = currentTime

                        // 플래시 효과
                        flashAlpha = 0.12f

                        // 번개 줄기 생성 (1-2개)
                        val boltCount = 1 + Random.nextInt(2)
                        lightningBolts = (0 until boltCount).map {
                            createTypingLightningBolt()
                        }

                        // 번개 페이드 아웃 애니메이션
                        repeat(10) {
                            delay(25)
                            flashAlpha *= 0.75f
                            lightningBolts = lightningBolts.map { bolt ->
                                bolt.copy(alpha = bolt.alpha * 0.8f)
                            }
                        }

                        lightningBolts = emptyList()
                        flashAlpha = 0f
                    }
                    delay(60)
                }
            } catch (e: CancellationException) {
                // 코루틴 취소는 정상 동작
                throw e
            } catch (e: Exception) {
                // 기타 예외 무시
            }
        }
        // isTyping이 false가 되면 상태 초기화
        lightningBolts = emptyList()
        flashAlpha = 0f
    }

    // 렌더링
    if (lightningBolts.isNotEmpty() || flashAlpha > 0.01f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            // 플래시 배경
            if (flashAlpha > 0.01f) {
                drawRect(
                    color = lightningColor.copy(alpha = flashAlpha),
                    size = size
                )
            }

            // 번개 줄기들
            lightningBolts.forEach { bolt ->
                drawTypingLightningBolt(bolt, lightningColor)
            }
        }
    }
}

/**
 * 번개 줄기 데이터
 */
private data class TypingLightningBolt(
    val segments: List<Offset>,  // 정규화된 좌표 (0~1)
    val width: Float,
    val alpha: Float,
    val branches: List<List<Offset>> = emptyList()
)

/**
 * 번개 줄기 생성
 */
private fun createTypingLightningBolt(): TypingLightningBolt {
    val segments = mutableListOf<Offset>()
    val random = Random

    // 시작점 (화면 상단 랜덤 위치)
    val startX = 0.1f + random.nextFloat() * 0.8f
    var currentX = startX
    var currentY = 0f

    segments.add(Offset(currentX, currentY))

    // 번개 경로 생성 (지그재그)
    val segmentCount = 6 + random.nextInt(5)
    val segmentHeight = 1f / segmentCount

    for (i in 1..segmentCount) {
        currentY = i * segmentHeight
        // 좌우로 랜덤하게 움직임
        currentX += (random.nextFloat() - 0.5f) * 0.25f
        currentX = currentX.coerceIn(0.05f, 0.95f)
        segments.add(Offset(currentX, currentY))
    }

    // 가지 번개 생성 (0-2개)
    val branches = mutableListOf<List<Offset>>()
    val branchCount = random.nextInt(3)
    for (b in 0 until branchCount) {
        val branchStartIdx = 2 + random.nextInt((segmentCount - 3).coerceAtLeast(1))
        if (branchStartIdx < segments.size) {
            val branchStart = segments[branchStartIdx]
            val branchSegments = mutableListOf(branchStart)

            var bx = branchStart.x
            var by = branchStart.y
            val direction = if (random.nextBoolean()) 1 else -1

            for (bs in 1..3) {
                bx += direction * (0.03f + random.nextFloat() * 0.04f)
                by += 0.04f + random.nextFloat() * 0.03f
                branchSegments.add(Offset(bx.coerceIn(0f, 1f), by.coerceIn(0f, 1f)))
            }
            branches.add(branchSegments)
        }
    }

    return TypingLightningBolt(
        segments = segments,
        width = 2.5f + random.nextFloat() * 1.5f,
        alpha = 1f,
        branches = branches
    )
}

/**
 * 번개 줄기 그리기
 */
private fun DrawScope.drawTypingLightningBolt(
    bolt: TypingLightningBolt,
    color: Color
) {
    if (bolt.segments.size < 2) return

    val path = Path()
    val firstPoint = bolt.segments.first()
    path.moveTo(firstPoint.x * size.width, firstPoint.y * size.height)

    bolt.segments.drop(1).forEach { point ->
        path.lineTo(point.x * size.width, point.y * size.height)
    }

    // 글로우 효과 (넓은 반투명)
    drawPath(
        path = path,
        color = color.copy(alpha = bolt.alpha * 0.25f),
        style = Stroke(width = bolt.width * 5f)
    )

    // 중간 글로우
    drawPath(
        path = path,
        color = color.copy(alpha = bolt.alpha * 0.5f),
        style = Stroke(width = bolt.width * 2.5f)
    )

    // 메인 번개 (밝은 색)
    drawPath(
        path = path,
        color = Color.White.copy(alpha = bolt.alpha * 0.9f),
        style = Stroke(width = bolt.width)
    )

    // 가지 번개
    bolt.branches.forEach { branch ->
        if (branch.size >= 2) {
            val branchPath = Path()
            branchPath.moveTo(branch.first().x * size.width, branch.first().y * size.height)
            branch.drop(1).forEach { point ->
                branchPath.lineTo(point.x * size.width, point.y * size.height)
            }

            drawPath(
                path = branchPath,
                color = color.copy(alpha = bolt.alpha * 0.35f),
                style = Stroke(width = bolt.width * 2f)
            )
            drawPath(
                path = branchPath,
                color = Color.White.copy(alpha = bolt.alpha * 0.7f),
                style = Stroke(width = bolt.width * 0.6f)
            )
        }
    }
}
