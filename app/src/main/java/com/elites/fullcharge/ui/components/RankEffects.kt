package com.elites.fullcharge.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elites.fullcharge.ui.theme.EliteGreen
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ============================================================
// 부사관 이펙트 (NCO Effects) - 로컬 전용, 상시 활성화
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

/**
 * 번개 테두리 Modifier
 * 말풍선 테두리에 전류가 흐르는 효과
 */
fun Modifier.lightningBorder(
    enabled: Boolean,
    color: Color = Color(0xFF00BFFF),
    strokeWidth: Dp = 1.5.dp
): Modifier {
    if (!enabled) return this

    return this.drawBehind {
        val width = size.width
        val height = size.height
        val stroke = strokeWidth.toPx()

        // 현재 시간 기반 랜덤 오프셋 (지지직 효과)
        val time = System.currentTimeMillis()
        val seed = (time / 100) % 1000
        val random = Random(seed)

        // 번개 경로 생성
        val path = Path()
        val segments = 12
        val segmentWidth = width / segments

        // 상단 번개
        path.moveTo(0f, random.nextFloat() * 3)
        for (i in 1..segments) {
            val x = i * segmentWidth
            val yOffset = (random.nextFloat() - 0.5f) * 4
            path.lineTo(x, yOffset)
        }

        drawPath(
            path = path,
            color = color.copy(alpha = 0.6f + random.nextFloat() * 0.3f),
            style = Stroke(width = stroke)
        )

        // 하단 번개
        val bottomPath = Path()
        bottomPath.moveTo(0f, height + random.nextFloat() * 3)
        for (i in 1..segments) {
            val x = i * segmentWidth
            val yOffset = height + (random.nextFloat() - 0.5f) * 4
            bottomPath.lineTo(x, yOffset)
        }

        drawPath(
            path = bottomPath,
            color = color.copy(alpha = 0.6f + random.nextFloat() * 0.3f),
            style = Stroke(width = stroke)
        )

        // 좌측 번개
        val leftPath = Path()
        val segmentHeight = height / 8
        leftPath.moveTo(random.nextFloat() * 3, 0f)
        for (i in 1..8) {
            val y = i * segmentHeight
            val xOffset = (random.nextFloat() - 0.5f) * 4
            leftPath.lineTo(xOffset, y)
        }

        drawPath(
            path = leftPath,
            color = color.copy(alpha = 0.5f + random.nextFloat() * 0.3f),
            style = Stroke(width = stroke * 0.8f)
        )

        // 우측 번개
        val rightPath = Path()
        rightPath.moveTo(width + random.nextFloat() * 3, 0f)
        for (i in 1..8) {
            val y = i * segmentHeight
            val xOffset = width + (random.nextFloat() - 0.5f) * 4
            rightPath.lineTo(xOffset, y)
        }

        drawPath(
            path = rightPath,
            color = color.copy(alpha = 0.5f + random.nextFloat() * 0.3f),
            style = Stroke(width = stroke * 0.8f)
        )
    }
}

/**
 * 전송 버스트 이펙트
 * 메시지 전송 시 버튼에서 전기 폭발 효과
 *
 * @param trigger 이펙트 트리거 여부
 * @param onComplete 이펙트 완료 콜백
 */
@Composable
fun SendBurstEffect(
    trigger: Boolean,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var burstParticles by remember { mutableStateOf(listOf<BurstParticle>()) }

    // 버스트 트리거
    LaunchedEffect(trigger) {
        if (trigger) {
            // 파티클 생성 (더 많이, 더 크게)
            burstParticles = (0 until 20).map { i ->
                val angle = (i * 18f) + Random.nextFloat() * 10f
                BurstParticle(
                    angle = angle,
                    distance = 0f,
                    speed = 10f + Random.nextFloat() * 8f,
                    size = 5f + Random.nextFloat() * 4f,
                    alpha = 1f,
                    color = if (i % 2 == 0) EliteGreen else Color(0xFF00BFFF)
                )
            }

            // 애니메이션 (더 오래)
            repeat(25) {
                delay(16)
                burstParticles = burstParticles.map { particle ->
                    particle.copy(
                        distance = particle.distance + particle.speed,
                        alpha = (particle.alpha * 0.88f).coerceAtLeast(0.2f),
                        size = particle.size * 0.96f
                    )
                }
            }

            burstParticles = emptyList()
            onComplete()
        }
    }

    // 항상 Canvas 렌더링
    Canvas(
        modifier = modifier.size(120.dp)
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2

        burstParticles.forEach { particle ->
            val x = centerX + cos(particle.angle * PI / 180).toFloat() * particle.distance
            val y = centerY + sin(particle.angle * PI / 180).toFloat() * particle.distance

            // 글로우 효과
            drawCircle(
                color = particle.color.copy(alpha = particle.alpha * 0.3f),
                radius = particle.size * 2f,
                center = Offset(x, y)
            )

            // 파티클 원
            drawCircle(
                color = particle.color.copy(alpha = particle.alpha),
                radius = particle.size,
                center = Offset(x, y)
            )

            // 파티클 꼬리 (더 두꺼움)
            val tailLength = particle.speed * 2f
            drawLine(
                color = particle.color.copy(alpha = particle.alpha * 0.7f),
                start = Offset(x, y),
                end = Offset(
                    centerX + cos(particle.angle * PI / 180).toFloat() * (particle.distance - tailLength),
                    centerY + sin(particle.angle * PI / 180).toFloat() * (particle.distance - tailLength)
                ),
                strokeWidth = particle.size * 0.9f
            )
        }
    }
}

private data class BurstParticle(
    val angle: Float,
    val distance: Float,
    val speed: Float,
    val size: Float,
    val alpha: Float,
    val color: Color
)

// ============================================================
// 장교 이펙트 (Officer Effects) - Firebase 동기화, 이벤트성
// ============================================================

/**
 * 도착 충격파 이펙트
 * 장교 메시지 도착 시 말풍선 주변으로 충격파 확산
 *
 * @param trigger 이펙트 트리거 여부
 * @param onComplete 이펙트 완료 콜백
 */
@Composable
fun ArrivalShockwaveEffect(
    trigger: Boolean,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var waves by remember { mutableStateOf(listOf<ShockWave>()) }

    LaunchedEffect(trigger) {
        if (trigger) {
            // 3개의 파동을 100ms 간격으로 생성
            repeat(3) { i ->
                delay(100L * i)
                waves = waves + ShockWave(
                    radius = 0f,
                    alpha = 0.8f,
                    strokeWidth = 3f
                )
            }

            // 애니메이션 (500ms 지속)
            repeat(30) {
                delay(16)
                waves = waves.mapNotNull { wave ->
                    val newRadius = wave.radius + 8f
                    val newAlpha = wave.alpha * 0.9f
                    if (newAlpha < 0.05f) null
                    else wave.copy(
                        radius = newRadius,
                        alpha = newAlpha,
                        strokeWidth = wave.strokeWidth * 0.95f
                    )
                }
            }

            waves = emptyList()
            onComplete()
        }
    }

    if (waves.isNotEmpty()) {
        Canvas(
            modifier = modifier.fillMaxSize()
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2

            waves.forEach { wave ->
                drawCircle(
                    color = Color(0xFFFFD700).copy(alpha = wave.alpha),
                    radius = wave.radius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = wave.strokeWidth)
                )
            }
        }
    }
}

private data class ShockWave(
    val radius: Float,
    val alpha: Float,
    val strokeWidth: Float
)

/**
 * 권위 펄스 이펙트
 * 장교 메시지 주변이 지속적으로 맥동
 */
@Composable
fun AuthorityPulseEffect(
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (!enabled) return

    val infiniteTransition = rememberInfiniteTransition(label = "authority_pulse")

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_radius"
    )

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        // 글로우 효과
        drawRect(
            color = Color(0xFFFFD700).copy(alpha = pulseAlpha * 0.3f),
            size = Size(
                size.width + pulseRadius * 2,
                size.height + pulseRadius * 2
            ),
            topLeft = Offset(-pulseRadius, -pulseRadius)
        )
    }
}

/**
 * 입장 장악 오버레이
 * 장교 입장 시 전체 화면 플래시
 *
 * @param officerNickname 장교 닉네임
 * @param officerRank 장교 계급
 * @param onDismiss 오버레이 닫기 콜백
 */
@Composable
fun EntranceTakeoverOverlay(
    visible: Boolean,
    officerNickname: String,
    officerRank: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var flashAlpha by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            // 플래시 인
            repeat(10) {
                flashAlpha = (it + 1) / 10f * 0.6f
                delay(30)
            }

            // 유지
            delay(1500)

            // 플래시 아웃
            repeat(15) {
                flashAlpha = 0.6f * (1f - (it + 1) / 15f)
                delay(30)
            }

            onDismiss()
        } else {
            flashAlpha = 0f
        }
    }

    if (flashAlpha > 0) {
        Box(
            modifier = modifier
                .fillMaxSize()
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                // 금색 플래시
                drawRect(
                    color = Color(0xFFFFD700).copy(alpha = flashAlpha),
                    size = size
                )
            }

            // 장교 정보 표시 (중앙)
            if (flashAlpha > 0.3f) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-20).dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = officerRank,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black.copy(alpha = (flashAlpha * 1.5f).coerceAtMost(1f))
                    )
                    Text(
                        text = "$officerNickname 입장",
                        fontSize = 18.sp,
                        color = Color.Black.copy(alpha = (flashAlpha * 1.3f).coerceAtMost(1f))
                    )
                }
            }
        }
    }
}
