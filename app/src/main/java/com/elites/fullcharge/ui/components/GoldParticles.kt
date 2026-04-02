package com.elites.fullcharge.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class GoldParticle(
    val id: Int,
    var x: Float,
    var y: Float,
    val size: Float,
    val speed: Float,
    val angle: Float,
    val alpha: Float
)

@Composable
fun GoldParticlesEffect(
    modifier: Modifier = Modifier,
    particleCount: Int = 50,
    isActive: Boolean = true
) {
    var particles by remember {
        mutableStateOf(
            List(particleCount) { index ->
                GoldParticle(
                    id = index,
                    x = Random.nextFloat(),
                    y = Random.nextFloat(),
                    size = Random.nextFloat() * 6f + 2f,
                    speed = Random.nextFloat() * 0.002f + 0.001f,
                    angle = Random.nextFloat() * 360f,
                    alpha = Random.nextFloat() * 0.7f + 0.3f
                )
            }
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleAnimation"
    )

    LaunchedEffect(animationProgress, isActive) {
        if (isActive) {
            particles = particles.map { particle ->
                val newY = particle.y - particle.speed
                val newX = particle.x + sin(particle.angle) * 0.001f

                if (newY < -0.1f) {
                    particle.copy(
                        x = Random.nextFloat(),
                        y = 1.1f,
                        alpha = Random.nextFloat() * 0.7f + 0.3f
                    )
                } else {
                    particle.copy(x = newX, y = newY)
                }
            }
        }
    }

    if (isActive) {
        Canvas(modifier = modifier.fillMaxSize()) {
            particles.forEach { particle ->
                drawCircle(
                    color = Color(0xFFFFD700).copy(alpha = particle.alpha),
                    radius = particle.size,
                    center = Offset(
                        x = particle.x * size.width,
                        y = particle.y * size.height
                    )
                )
            }
        }
    }
}

@Composable
fun GoldBorderGlow(
    modifier: Modifier = Modifier,
    isActive: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAnimation"
    )

    if (isActive) {
        Canvas(modifier = modifier.fillMaxSize()) {
            // Top edge glow
            drawRect(
                color = Color(0xFFFFD700).copy(alpha = glowAlpha * 0.5f),
                topLeft = Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(size.width, 20f)
            )
            // Bottom edge glow
            drawRect(
                color = Color(0xFFFFD700).copy(alpha = glowAlpha * 0.5f),
                topLeft = Offset(0f, size.height - 20f),
                size = androidx.compose.ui.geometry.Size(size.width, 20f)
            )
            // Left edge glow
            drawRect(
                color = Color(0xFFFFD700).copy(alpha = glowAlpha * 0.3f),
                topLeft = Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(15f, size.height)
            )
            // Right edge glow
            drawRect(
                color = Color(0xFFFFD700).copy(alpha = glowAlpha * 0.3f),
                topLeft = Offset(size.width - 15f, 0f),
                size = androidx.compose.ui.geometry.Size(15f, size.height)
            )
        }
    }
}
