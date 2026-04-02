package com.elites.fullcharge.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 풀파워 모드 - RGB 무지개 배경
 * "넌 충전 중이잖아. 배터리 아낄 필요 없어."
 */
@Composable
fun RgbRainbowBackground(
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    if (!enabled) return

    val infiniteTransition = rememberInfiniteTransition(label = "무지개")

    val hue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "색상회전"
    )

    val color1 = Color.hsl(hue, 0.8f, 0.5f)
    val color2 = Color.hsl((hue + 60) % 360, 0.8f, 0.5f)
    val color3 = Color.hsl((hue + 120) % 360, 0.8f, 0.5f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        color1.copy(alpha = 0.15f),
                        color2.copy(alpha = 0.1f),
                        color3.copy(alpha = 0.15f)
                    )
                )
            )
    )
}

/**
 * 번개 이펙트 - 랜덤하게 화면에 번개가 친다
 */
@Composable
fun LightningEffect(
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    if (!enabled) return

    var lightningVisible by remember { mutableStateOf(false) }
    var lightningPath by remember { mutableStateOf<List<Offset>>(emptyList()) }

    LaunchedEffect(Unit) {
        while (true) {
            // 3~8초 랜덤 간격으로 번개
            kotlinx.coroutines.delay(Random.nextLong(3000, 8000))
            lightningPath = generateLightningPath()
            lightningVisible = true
            kotlinx.coroutines.delay(100)
            lightningVisible = false
            kotlinx.coroutines.delay(50)
            lightningVisible = true
            kotlinx.coroutines.delay(80)
            lightningVisible = false
        }
    }

    if (lightningVisible) {
        Canvas(modifier = modifier.fillMaxSize()) {
            drawLightning(lightningPath, size.width, size.height)
        }
    }
}

private fun generateLightningPath(): List<Offset> {
    val points = mutableListOf<Offset>()
    var x = Random.nextFloat()
    var y = 0f

    points.add(Offset(x, y))

    while (y < 1f) {
        x += Random.nextFloat() * 0.2f - 0.1f
        y += Random.nextFloat() * 0.15f + 0.05f
        points.add(Offset(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f)))
    }

    return points
}

private fun DrawScope.drawLightning(path: List<Offset>, width: Float, height: Float) {
    if (path.size < 2) return

    val scaledPath = path.map { Offset(it.x * width, it.y * height) }

    // 외부 글로우
    for (i in 0 until scaledPath.size - 1) {
        drawLine(
            color = Color(0xFFFFD700).copy(alpha = 0.3f),
            start = scaledPath[i],
            end = scaledPath[i + 1],
            strokeWidth = 20f
        )
    }

    // 내부 번개
    for (i in 0 until scaledPath.size - 1) {
        drawLine(
            color = Color.White,
            start = scaledPath[i],
            end = scaledPath[i + 1],
            strokeWidth = 4f
        )
    }
}

/**
 * 강화된 금가루 이펙트 - 더 많은 파티클, 더 밝은 색상
 */
@Composable
fun IntenseGoldParticles(
    modifier: Modifier = Modifier,
    particleCount: Int = 100,
    enabled: Boolean = true
) {
    if (!enabled) return

    data class 파티클(
        val id: Int,
        var x: Float,
        var y: Float,
        val 크기: Float,
        val 속도: Float,
        val 회전: Float,
        val 밝기: Float,
        val 색상타입: Int // 0: 금색, 1: 흰색, 2: 주황
    )

    var particles by remember {
        mutableStateOf(
            List(particleCount) { index ->
                파티클(
                    id = index,
                    x = Random.nextFloat(),
                    y = Random.nextFloat(),
                    크기 = Random.nextFloat() * 8f + 3f,
                    속도 = Random.nextFloat() * 0.003f + 0.001f,
                    회전 = Random.nextFloat() * 360f,
                    밝기 = Random.nextFloat() * 0.5f + 0.5f,
                    색상타입 = Random.nextInt(3)
                )
            }
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "파티클")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "파티클애니메이션"
    )

    LaunchedEffect(animationProgress) {
        particles = particles.map { p ->
            val newY = p.y - p.속도
            val newX = p.x + sin(p.회전) * 0.002f

            if (newY < -0.1f) {
                p.copy(
                    x = Random.nextFloat(),
                    y = 1.1f,
                    밝기 = Random.nextFloat() * 0.5f + 0.5f
                )
            } else {
                p.copy(x = newX, y = newY)
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            val color = when (p.색상타입) {
                0 -> Color(0xFFFFD700) // 금색
                1 -> Color(0xFFFFFFFF) // 흰색
                else -> Color(0xFFFF8C00) // 주황
            }

            drawCircle(
                color = color.copy(alpha = p.밝기),
                radius = p.크기,
                center = Offset(p.x * size.width, p.y * size.height)
            )

            // 빛나는 효과
            drawCircle(
                color = color.copy(alpha = p.밝기 * 0.3f),
                radius = p.크기 * 2f,
                center = Offset(p.x * size.width, p.y * size.height)
            )
        }
    }
}

/**
 * 펄스 글로우 테두리 - 화면 가장자리가 금색으로 펄스
 */
@Composable
fun PulseGlowBorder(
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    if (!enabled) return

    val infiniteTransition = rememberInfiniteTransition(label = "펄스")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "펄스알파"
    )

    val pulseSize by infiniteTransition.animateFloat(
        initialValue = 30f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "펄스크기"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // 상단
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFD700).copy(alpha = pulseAlpha),
                    Color.Transparent
                )
            ),
            size = androidx.compose.ui.geometry.Size(size.width, pulseSize)
        )

        // 하단
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFFFD700).copy(alpha = pulseAlpha)
                )
            ),
            topLeft = Offset(0f, size.height - pulseSize),
            size = androidx.compose.ui.geometry.Size(size.width, pulseSize)
        )

        // 좌측
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFFFFD700).copy(alpha = pulseAlpha * 0.7f),
                    Color.Transparent
                )
            ),
            size = androidx.compose.ui.geometry.Size(pulseSize * 0.7f, size.height)
        )

        // 우측
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFFFD700).copy(alpha = pulseAlpha * 0.7f)
                )
            ),
            topLeft = Offset(size.width - pulseSize * 0.7f, 0f),
            size = androidx.compose.ui.geometry.Size(pulseSize * 0.7f, size.height)
        )
    }
}
