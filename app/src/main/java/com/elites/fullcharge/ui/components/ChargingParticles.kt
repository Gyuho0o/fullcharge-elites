package com.elites.fullcharge.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.elites.fullcharge.ui.theme.EliteGreen
import kotlin.random.Random

/**
 * 충전 중일 때 표시되는 파티클 효과
 * 초록색 파티클이 아래에서 위로 떠오르는 효과
 */
@Composable
fun ChargingParticles(
    modifier: Modifier = Modifier,
    particleCount: Int = 30,
    color: Color = EliteGreen.copy(alpha = 0.6f)
) {
    data class Particle(
        val id: Int,
        var x: Float,
        var y: Float,
        val size: Float,
        val speed: Float,
        val alpha: Float
    )

    var particles by remember(particleCount) {
        mutableStateOf(
            List(particleCount) { index ->
                Particle(
                    id = index,
                    x = Random.nextFloat(),
                    y = Random.nextFloat(),
                    size = Random.nextFloat() * 4f + 1f,
                    speed = Random.nextFloat() * 0.002f + 0.001f,
                    alpha = Random.nextFloat() * 0.4f + 0.1f
                )
            }
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    LaunchedEffect(time) {
        particles = particles.map { p ->
            val newY = p.y - p.speed
            if (newY < -0.05f) {
                p.copy(x = Random.nextFloat(), y = 1.05f)
            } else {
                p.copy(y = newY)
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            // 글로우
            drawCircle(
                color = color.copy(alpha = p.alpha * 0.3f),
                radius = p.size * 2f,
                center = Offset(p.x * size.width, p.y * size.height)
            )
            // 코어
            drawCircle(
                color = color.copy(alpha = p.alpha),
                radius = p.size,
                center = Offset(p.x * size.width, p.y * size.height)
            )
        }
    }
}
