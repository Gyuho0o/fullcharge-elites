package com.elites.fullcharge.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elites.fullcharge.data.RankEffect
import com.elites.fullcharge.ui.theme.EliteGreen
import com.elites.fullcharge.ui.theme.TossBlueLight
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 이펙트 오버레이 - 채팅방에서 이펙트 수신 시 전체 화면에 표시
 */
@Composable
fun EffectOverlay(
    effectType: RankEffect.EffectType?,
    senderNickname: String,
    onEffectComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (effectType == null) return

    var isPlaying by remember(effectType) { mutableStateOf(true) }

    LaunchedEffect(effectType) {
        delay(effectType.durationMs)
        isPlaying = false
        onEffectComplete()
    }

    if (isPlaying) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (effectType) {
                // 부사관 이펙트
                RankEffect.EffectType.CHARGE -> ChargeEffect()
                RankEffect.EffectType.BULB -> BulbEffect()
                RankEffect.EffectType.PLUG -> PlugEffect()
                RankEffect.EffectType.FULL_CHARGE -> FullChargeEffect(senderNickname)
                RankEffect.EffectType.CURRENT -> CurrentEffect()
                RankEffect.EffectType.POWER_SURGE -> PowerSurgeEffect()
                // 장교 이펙트
                RankEffect.EffectType.LIGHTNING_STORM -> LightningStormEffect()
                RankEffect.EffectType.ENERGY_BURST -> EnergyBurstEffect()
                RankEffect.EffectType.DOUBLE_LIGHTNING -> DoubleLightningEffect()
                RankEffect.EffectType.THUNDERBOLT -> ThunderboltEffect()
                RankEffect.EffectType.OVERCHARGE -> OverchargeEffect()
            }
        }
    }
}

/**
 * 스파크 이펙트 - 번개가 찌직 튀는 효과
 */
@Composable
private fun SparkEffect() {
    var sparks by remember { mutableStateOf<List<SparkParticle>>(emptyList()) }
    var flashAlpha by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        // 3번 연속 스파크
        repeat(3) {
            sparks = List(8) { generateSparkParticle() }
            flashAlpha = 0.2f
            delay(60)
            flashAlpha = 0f
            delay(80)
        }
        sparks = emptyList()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        // 플래시
        if (flashAlpha > 0) {
            drawRect(color = TossBlueLight.copy(alpha = flashAlpha))
        }

        // 스파크 파티클
        sparks.forEach { spark ->
            drawSparkParticle(spark)
        }
    }
}

/**
 * 충전 이펙트 - 배터리가 차오르는 효과
 */
@Composable
private fun ChargeEffect() {
    val chargeProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "charge"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (chargeProgress >= 1f) 0.8f else 0.3f,
        animationSpec = tween(200),
        label = "glow"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .width(200.dp)
                .height(100.dp)
        ) {
            val batteryWidth = size.width * 0.8f
            val batteryHeight = size.height * 0.6f
            val left = (size.width - batteryWidth) / 2
            val top = (size.height - batteryHeight) / 2

            // 배터리 외곽 글로우
            drawRoundRect(
                color = EliteGreen.copy(alpha = glowAlpha * 0.3f),
                topLeft = Offset(left - 8, top - 8),
                size = Size(batteryWidth + 16, batteryHeight + 16),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f)
            )

            // 배터리 외곽선
            drawRoundRect(
                color = EliteGreen,
                topLeft = Offset(left, top),
                size = Size(batteryWidth, batteryHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f),
                style = Stroke(width = 4f)
            )

            // 배터리 팁
            drawRoundRect(
                color = EliteGreen,
                topLeft = Offset(left + batteryWidth, top + batteryHeight * 0.25f),
                size = Size(20f, batteryHeight * 0.5f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
            )

            // 충전량
            val fillWidth = (batteryWidth - 16) * chargeProgress
            drawRoundRect(
                color = EliteGreen.copy(alpha = 0.8f),
                topLeft = Offset(left + 8, top + 8),
                size = Size(fillWidth, batteryHeight - 16),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
            )
        }

        // 퍼센트 텍스트
        Text(
            text = "${(chargeProgress * 100).toInt()}%",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (chargeProgress > 0.5f) Color.Black else EliteGreen
        )
    }
}

/**
 * 전구 이펙트 - 전구가 반짝 켜지는 효과
 */
@Composable
private fun BulbEffect() {
    var bulbOn by remember { mutableStateOf(false) }
    var flashAlpha by remember { mutableFloatStateOf(0f) }

    val glowRadius by animateFloatAsState(
        targetValue = if (bulbOn) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "glow"
    )

    LaunchedEffect(Unit) {
        delay(100)
        bulbOn = true
        flashAlpha = 0.3f
        delay(100)
        flashAlpha = 0f
        delay(400)
        bulbOn = false
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 플래시
            if (flashAlpha > 0) {
                drawRect(color = Color(0xFFFFEB3B).copy(alpha = flashAlpha))
            }

            val centerX = size.width / 2
            val centerY = size.height / 2

            // 글로우 효과
            if (bulbOn) {
                val maxRadius = size.minDimension * 0.4f * glowRadius
                for (i in 5 downTo 1) {
                    drawCircle(
                        color = Color(0xFFFFEB3B).copy(alpha = 0.1f * i / 5),
                        radius = maxRadius * i / 5,
                        center = Offset(centerX, centerY)
                    )
                }
            }
        }

        // 전구 이모지
        Text(
            text = "💡",
            fontSize = if (bulbOn) 80.sp else 60.sp,
            modifier = Modifier.offset(y = (-20).dp)
        )
    }
}

/**
 * 플러그 이펙트 - 플러그 꽂히며 스파크
 */
@Composable
private fun PlugEffect() {
    var pluggedIn by remember { mutableStateOf(false) }
    var sparks by remember { mutableStateOf<List<SparkParticle>>(emptyList()) }

    val plugOffset by animateFloatAsState(
        targetValue = if (pluggedIn) 0f else 50f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "plug"
    )

    LaunchedEffect(Unit) {
        delay(100)
        pluggedIn = true
        delay(150)
        // 스파크 발생
        repeat(2) {
            sparks = List(6) { generateSparkParticle() }
            delay(80)
        }
        sparks = emptyList()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            sparks.forEach { spark ->
                drawSparkParticle(spark)
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🔌", fontSize = 60.sp)
            Text(
                text = "⚡",
                fontSize = 40.sp,
                modifier = Modifier.offset(x = plugOffset.dp)
            )
        }
    }
}

/**
 * 번개 폭풍 이펙트 - 여러 번개가 동시에 내리침
 */
@Composable
private fun LightningStormEffect() {
    var lightnings by remember { mutableStateOf<List<LightningBoltData>>(emptyList()) }
    var flashAlpha by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        repeat(5) {
            lightnings = List(Random.nextInt(3, 6)) { generateLightningBoltData() }
            flashAlpha = 0.4f
            delay(80)
            flashAlpha = 0.1f
            delay(60)
            flashAlpha = 0f
            delay(100)
        }
        lightnings = emptyList()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        // 플래시
        if (flashAlpha > 0) {
            drawRect(color = TossBlueLight.copy(alpha = flashAlpha))
        }

        // 번개들
        lightnings.forEach { bolt ->
            drawLightningBoltData(bolt, TossBlueLight)
        }
    }
}

/**
 * 에너지 버스트 이펙트 - 원형 충격파가 퍼져나감
 */
@Composable
private fun EnergyBurstEffect() {
    // 3개 웨이브의 시작 시간 (stagger)
    data class WaveState(val startTime: Long, var progress: Float)

    var waves by remember { mutableStateOf<List<WaveState>>(emptyList()) }
    var flashAlpha by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        // 웨이브 3개 순차적으로 시작
        val startTime = System.currentTimeMillis()
        waves = listOf(
            WaveState(startTime, 0f),
            WaveState(startTime + 300, 0f),
            WaveState(startTime + 600, 0f)
        )

        // 플래시
        flashAlpha = 0.3f
        delay(100)
        flashAlpha = 0f

        // 애니메이션 업데이트
        val duration = 1200L
        while (true) {
            val now = System.currentTimeMillis()
            var allDone = true
            waves = waves.map { wave ->
                val elapsed = now - wave.startTime
                val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                if (progress < 1f) allDone = false
                wave.copy(progress = progress)
            }
            if (allDone) break
            delay(16) // ~60fps
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = size.minDimension * 0.6f

        // 플래시
        if (flashAlpha > 0) {
            drawRect(color = EliteGreen.copy(alpha = flashAlpha))
        }

        waves.forEach { wave ->
            if (wave.progress > 0f) {
                val radius = maxRadius * wave.progress
                val alpha = (1f - wave.progress) * 0.7f
                val strokeWidth = 12f * (1f - wave.progress * 0.5f)

                // 외부 글로우
                drawCircle(
                    color = EliteGreen.copy(alpha = alpha * 0.3f),
                    radius = radius + 10,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = strokeWidth * 2)
                )

                // 메인 웨이브
                drawCircle(
                    color = EliteGreen.copy(alpha = alpha),
                    radius = radius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = strokeWidth)
                )

                // 밝은 중심
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.5f),
                    radius = radius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = strokeWidth * 0.5f)
                )
            }
        }
    }
}

/**
 * 더블 라이트닝 이펙트 - 양쪽에서 번개가 교차
 */
@Composable
private fun DoubleLightningEffect() {
    var leftBolts by remember { mutableStateOf<List<LightningBoltData>>(emptyList()) }
    var rightBolts by remember { mutableStateOf<List<LightningBoltData>>(emptyList()) }
    var flashAlpha by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        repeat(3) {
            leftBolts = List(2) { generateSideLightning(fromLeft = true) }
            rightBolts = List(2) { generateSideLightning(fromLeft = false) }
            flashAlpha = 0.5f
            delay(100)
            flashAlpha = 0.2f
            delay(80)
            flashAlpha = 0f
            delay(120)
        }
        leftBolts = emptyList()
        rightBolts = emptyList()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        // 플래시
        if (flashAlpha > 0) {
            drawRect(color = TossBlueLight.copy(alpha = flashAlpha))
        }

        // 좌측 번개
        leftBolts.forEach { bolt ->
            drawLightningBoltData(bolt, TossBlueLight)
        }

        // 우측 번개
        rightBolts.forEach { bolt ->
            drawLightningBoltData(bolt, TossBlueLight)
        }
    }
}

/**
 * 완충 선언 이펙트 - 화면 번쩍 + "완충!" 텍스트 + 금색 파티클
 */
@Composable
private fun FullChargeEffect(senderNickname: String) {
    var flashAlpha by remember { mutableFloatStateOf(0f) }
    var showText by remember { mutableStateOf(false) }
    var particles by remember { mutableStateOf<List<GoldParticleData>>(emptyList()) }

    val textScale by animateFloatAsState(
        targetValue = if (showText) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "textScale"
    )

    LaunchedEffect(Unit) {
        // 플래시
        flashAlpha = 0.8f
        delay(100)
        flashAlpha = 0.3f
        showText = true
        particles = List(30) { generateGoldParticle() }
        delay(100)
        flashAlpha = 0f
    }

    // 파티클 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val particleTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleTime"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 플래시
            if (flashAlpha > 0) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = flashAlpha),
                            Color(0xFFFFD700).copy(alpha = flashAlpha * 0.5f),
                            Color.Transparent
                        ),
                        center = Offset(size.width / 2, size.height / 2),
                        radius = size.maxDimension
                    )
                )
            }

            // 금색 파티클
            particles.forEach { particle ->
                val progress = (particleTime + particle.delay) % 1f
                val y = particle.startY - progress * 0.5f
                val x = particle.startX + sin(progress * 6.28f + particle.phase) * 0.05f
                val alpha = (1f - progress) * particle.alpha

                if (y > 0) {
                    // 글로우
                    drawCircle(
                        color = Color(0xFFFFD700).copy(alpha = alpha * 0.3f),
                        radius = particle.size * 2f,
                        center = Offset(x * size.width, y * size.height)
                    )
                    // 파티클
                    drawCircle(
                        color = Color(0xFFFFD700).copy(alpha = alpha),
                        radius = particle.size,
                        center = Offset(x * size.width, y * size.height)
                    )
                }
            }
        }

        // 텍스트
        if (showText) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .scale(textScale)
            ) {
                Text(
                    text = "👑",
                    fontSize = 60.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "완충!",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD700)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = senderNickname,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * 전류 이펙트 - 전기가 흐르는 라인 효과
 */
@Composable
private fun CurrentEffect() {
    var electricLines by remember { mutableStateOf<List<ElectricLineData>>(emptyList()) }
    var flashAlpha by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        repeat(4) {
            electricLines = List(6) { generateElectricLine() }
            flashAlpha = 0.2f
            delay(80)
            flashAlpha = 0f
            delay(100)
        }
        electricLines = emptyList()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        // 플래시
        if (flashAlpha > 0) {
            drawRect(color = Color(0xFF00FFFF).copy(alpha = flashAlpha))
        }

        // 전류 라인
        electricLines.forEach { line ->
            drawElectricLine(line)
        }
    }
}

/**
 * 파워 서지 이펙트 - 강력한 전력 폭발
 */
@Composable
private fun PowerSurgeEffect() {
    var surgeProgress by remember { mutableFloatStateOf(0f) }
    var flashAlpha by remember { mutableFloatStateOf(0f) }
    var rays by remember { mutableStateOf<List<RayData>>(emptyList()) }

    LaunchedEffect(Unit) {
        rays = List(12) { generateRay(it * 30f) }

        // 충전
        val chargeStart = System.currentTimeMillis()
        val chargeDuration = 600L
        while (surgeProgress < 1f) {
            val elapsed = System.currentTimeMillis() - chargeStart
            surgeProgress = (elapsed.toFloat() / chargeDuration).coerceIn(0f, 1f)
            delay(16)
        }

        // 폭발
        flashAlpha = 0.6f
        delay(100)
        flashAlpha = 0.3f
        delay(100)
        flashAlpha = 0f
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2

        // 플래시
        if (flashAlpha > 0) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = flashAlpha),
                        Color(0xFF00FFFF).copy(alpha = flashAlpha * 0.5f),
                        Color.Transparent
                    ),
                    center = Offset(centerX, centerY),
                    radius = size.maxDimension
                )
            )
        }

        // 중심 에너지 구
        val coreRadius = 40f + surgeProgress * 30f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White,
                    Color(0xFF00FFFF),
                    Color(0xFF0066FF).copy(alpha = 0.5f),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = coreRadius * 2
            ),
            radius = coreRadius * 2,
            center = Offset(centerX, centerY)
        )

        // 광선
        rays.forEach { ray ->
            val rayLength = surgeProgress * size.minDimension * 0.4f
            val angleRad = Math.toRadians(ray.angle.toDouble())
            val endX = centerX + cos(angleRad).toFloat() * rayLength
            val endY = centerY + sin(angleRad).toFloat() * rayLength

            // 글로우
            drawLine(
                color = Color(0xFF00FFFF).copy(alpha = 0.3f * surgeProgress),
                start = Offset(centerX, centerY),
                end = Offset(endX, endY),
                strokeWidth = 16f,
                cap = StrokeCap.Round
            )

            // 메인
            drawLine(
                color = Color(0xFF00FFFF).copy(alpha = 0.7f * surgeProgress),
                start = Offset(centerX, centerY),
                end = Offset(endX, endY),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )

            // 밝은 중심
            drawLine(
                color = Color.White.copy(alpha = surgeProgress),
                start = Offset(centerX, centerY),
                end = Offset(endX, endY),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * 썬더볼트 이펙트 - 거대한 번개 낙뢰
 */
@Composable
private fun ThunderboltEffect() {
    var mainBolt by remember { mutableStateOf<LightningBoltData?>(null) }
    var sideBolts by remember { mutableStateOf<List<LightningBoltData>>(emptyList()) }
    var flashAlpha by remember { mutableFloatStateOf(0f) }
    var impactRipple by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        // 메인 번개
        mainBolt = generateThunderBolt()
        flashAlpha = 0.7f
        delay(80)
        flashAlpha = 0.3f

        // 사이드 번개
        sideBolts = List(3) { generateBranchBolt(mainBolt!!) }
        delay(60)
        flashAlpha = 0.5f

        // 충격파
        val rippleStart = System.currentTimeMillis()
        val rippleDuration = 800L
        while (impactRipple < 1f) {
            val elapsed = System.currentTimeMillis() - rippleStart
            impactRipple = (elapsed.toFloat() / rippleDuration).coerceIn(0f, 1f)
            delay(16)
        }

        flashAlpha = 0f
        delay(200)
        mainBolt = null
        sideBolts = emptyList()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        // 플래시
        if (flashAlpha > 0) {
            drawRect(color = Color(0xFF6666FF).copy(alpha = flashAlpha))
        }

        // 충격파
        if (impactRipple > 0f) {
            val rippleRadius = size.minDimension * 0.5f * impactRipple
            val rippleAlpha = (1f - impactRipple) * 0.5f
            drawCircle(
                color = Color.White.copy(alpha = rippleAlpha),
                radius = rippleRadius,
                center = Offset(size.width / 2, size.height * 0.7f),
                style = Stroke(width = 8f * (1f - impactRipple * 0.5f))
            )
        }

        // 메인 번개
        mainBolt?.let { bolt ->
            drawThunderBolt(bolt, Color(0xFF8888FF), strokeWidth = 8f)
        }

        // 사이드 번개
        sideBolts.forEach { bolt ->
            drawLightningBoltData(bolt, Color(0xFFAAAAFF))
        }
    }
}

/**
 * 오버차지 이펙트 - 과충전 폭발
 */
@Composable
private fun OverchargeEffect() {
    var chargeProgress by remember { mutableFloatStateOf(0f) }
    var explosionProgress by remember { mutableFloatStateOf(0f) }
    var flashAlpha by remember { mutableFloatStateOf(0f) }
    var sparks by remember { mutableStateOf<List<ExplosionSparkData>>(emptyList()) }
    var shakeOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(Unit) {
        // 충전 단계 (흔들림과 함께)
        val chargeStart = System.currentTimeMillis()
        val chargeDuration = 1200L
        while (chargeProgress < 1f) {
            val elapsed = System.currentTimeMillis() - chargeStart
            chargeProgress = (elapsed.toFloat() / chargeDuration).coerceIn(0f, 1f)
            // 점점 강해지는 흔들림
            val shake = chargeProgress * 8f
            shakeOffset = Offset(
                (Random.nextFloat() - 0.5f) * shake,
                (Random.nextFloat() - 0.5f) * shake
            )
            delay(16)
        }
        shakeOffset = Offset.Zero

        // 폭발
        sparks = List(40) { generateExplosionSpark() }
        flashAlpha = 1f
        delay(80)
        flashAlpha = 0.5f

        // 폭발 애니메이션
        val explosionStart = System.currentTimeMillis()
        val explosionDuration = 1000L
        while (explosionProgress < 1f) {
            val elapsed = System.currentTimeMillis() - explosionStart
            explosionProgress = (elapsed.toFloat() / explosionDuration).coerceIn(0f, 1f)
            flashAlpha = 0.5f * (1f - explosionProgress)
            delay(16)
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .offset(x = shakeOffset.x.dp, y = shakeOffset.y.dp)
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2

        // 플래시
        if (flashAlpha > 0) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = flashAlpha),
                        Color(0xFFFF6600).copy(alpha = flashAlpha * 0.7f),
                        Color(0xFFFF0000).copy(alpha = flashAlpha * 0.3f),
                        Color.Transparent
                    ),
                    center = Offset(centerX, centerY),
                    radius = size.maxDimension
                )
            )
        }

        // 충전 중 에너지 구
        if (chargeProgress > 0f && explosionProgress == 0f) {
            val coreRadius = 30f + chargeProgress * 50f
            // 불안정한 외부 글로우
            for (i in 3 downTo 1) {
                val jitter = (Random.nextFloat() - 0.5f) * chargeProgress * 20f
                drawCircle(
                    color = Color(0xFFFF6600).copy(alpha = 0.2f * i / 3),
                    radius = coreRadius * (1.5f + i * 0.3f) + jitter,
                    center = Offset(centerX, centerY)
                )
            }
            // 코어
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        Color(0xFFFFAA00),
                        Color(0xFFFF6600),
                        Color(0xFFFF0000).copy(alpha = 0.5f)
                    ),
                    center = Offset(centerX, centerY),
                    radius = coreRadius
                ),
                radius = coreRadius,
                center = Offset(centerX, centerY)
            )
        }

        // 폭발 스파크
        if (explosionProgress > 0f) {
            sparks.forEach { spark ->
                val distance = explosionProgress * size.minDimension * 0.6f * spark.speed
                val angleRad = Math.toRadians(spark.angle.toDouble())
                val x = centerX + cos(angleRad).toFloat() * distance
                val y = centerY + sin(angleRad).toFloat() * distance
                val alpha = (1f - explosionProgress) * spark.alpha
                val sparkSize = spark.size * (1f - explosionProgress * 0.5f)

                // 트레일
                val trailLength = distance * 0.3f
                val trailStartX = x - cos(angleRad).toFloat() * trailLength
                val trailStartY = y - sin(angleRad).toFloat() * trailLength

                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            spark.color.copy(alpha = alpha * 0.5f),
                            spark.color.copy(alpha = alpha)
                        ),
                        start = Offset(trailStartX, trailStartY),
                        end = Offset(x, y)
                    ),
                    start = Offset(trailStartX, trailStartY),
                    end = Offset(x, y),
                    strokeWidth = sparkSize,
                    cap = StrokeCap.Round
                )

                // 스파크 헤드
                drawCircle(
                    color = spark.color.copy(alpha = alpha),
                    radius = sparkSize,
                    center = Offset(x, y)
                )
            }
        }
    }
}

// ========== 헬퍼 데이터 클래스 및 함수 ==========

private data class SparkParticle(
    val centerX: Float,
    val centerY: Float,
    val angle: Float,
    val length: Float,
    val alpha: Float
)

private data class LightningBoltData(
    val points: List<Offset>
)

private data class GoldParticleData(
    val startX: Float,
    val startY: Float,
    val size: Float,
    val alpha: Float,
    val delay: Float,
    val phase: Float
)

private fun generateSparkParticle(): SparkParticle {
    return SparkParticle(
        centerX = 0.5f + (Random.nextFloat() - 0.5f) * 0.3f,
        centerY = 0.5f + (Random.nextFloat() - 0.5f) * 0.3f,
        angle = Random.nextFloat() * 360f,
        length = Random.nextFloat() * 0.1f + 0.05f,
        alpha = Random.nextFloat() * 0.5f + 0.5f
    )
}

private fun DrawScope.drawSparkParticle(spark: SparkParticle) {
    val centerX = spark.centerX * size.width
    val centerY = spark.centerY * size.height
    val angleRad = Math.toRadians(spark.angle.toDouble())
    val length = spark.length * size.minDimension

    val endX = centerX + cos(angleRad).toFloat() * length
    val endY = centerY + sin(angleRad).toFloat() * length

    // 글로우
    drawLine(
        color = TossBlueLight.copy(alpha = spark.alpha * 0.3f),
        start = Offset(centerX, centerY),
        end = Offset(endX, endY),
        strokeWidth = 8f,
        cap = StrokeCap.Round
    )

    // 메인
    drawLine(
        color = TossBlueLight.copy(alpha = spark.alpha),
        start = Offset(centerX, centerY),
        end = Offset(endX, endY),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )

    // 밝은 중심
    drawLine(
        color = Color.White.copy(alpha = spark.alpha),
        start = Offset(centerX, centerY),
        end = Offset(endX, endY),
        strokeWidth = 1.5f,
        cap = StrokeCap.Round
    )
}

private fun generateLightningBoltData(): LightningBoltData {
    val startX = Random.nextFloat() * 0.6f + 0.2f
    val points = mutableListOf<Offset>()
    var x = startX
    var y = 0f

    points.add(Offset(x, y))

    while (y < 1f) {
        x += (Random.nextFloat() - 0.5f) * 0.15f
        y += Random.nextFloat() * 0.15f + 0.08f
        points.add(Offset(x.coerceIn(0.05f, 0.95f), y.coerceIn(0f, 1f)))
    }

    return LightningBoltData(points)
}

private fun generateSideLightning(fromLeft: Boolean): LightningBoltData {
    val startX = if (fromLeft) 0f else 1f
    val endX = if (fromLeft) 0.5f + Random.nextFloat() * 0.3f else 0.5f - Random.nextFloat() * 0.3f
    val startY = Random.nextFloat() * 0.3f + 0.2f

    val points = mutableListOf<Offset>()
    var x = startX
    var y = startY

    points.add(Offset(x, y))

    val steps = Random.nextInt(4, 7)
    val xStep = (endX - startX) / steps

    repeat(steps) {
        x += xStep + (Random.nextFloat() - 0.5f) * 0.05f
        y += (Random.nextFloat() - 0.5f) * 0.1f
        points.add(Offset(x.coerceIn(0f, 1f), y.coerceIn(0.1f, 0.9f)))
    }

    return LightningBoltData(points)
}

private fun DrawScope.drawLightningBoltData(bolt: LightningBoltData, color: Color) {
    val scaledPoints = bolt.points.map {
        Offset(it.x * size.width, it.y * size.height)
    }

    for (i in 0 until scaledPoints.size - 1) {
        // 글로우
        drawLine(
            color = color.copy(alpha = 0.2f),
            start = scaledPoints[i],
            end = scaledPoints[i + 1],
            strokeWidth = 12f,
            cap = StrokeCap.Round
        )

        // 메인
        drawLine(
            color = color.copy(alpha = 0.6f),
            start = scaledPoints[i],
            end = scaledPoints[i + 1],
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )

        // 밝은 중심
        drawLine(
            color = Color.White.copy(alpha = 0.8f),
            start = scaledPoints[i],
            end = scaledPoints[i + 1],
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
    }
}

private fun generateGoldParticle(): GoldParticleData {
    return GoldParticleData(
        startX = Random.nextFloat(),
        startY = Random.nextFloat() * 0.5f + 0.5f,
        size = Random.nextFloat() * 6f + 3f,
        alpha = Random.nextFloat() * 0.5f + 0.5f,
        delay = Random.nextFloat(),
        phase = Random.nextFloat() * 6.28f
    )
}

// ========== 전류 이펙트 헬퍼 ==========

private data class ElectricLineData(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val segments: List<Offset>,
    val alpha: Float
)

private fun generateElectricLine(): ElectricLineData {
    val startX = Random.nextFloat()
    val startY = Random.nextFloat()
    val angle = Random.nextFloat() * 360f
    val length = Random.nextFloat() * 0.3f + 0.1f
    val angleRad = Math.toRadians(angle.toDouble())
    val endX = (startX + cos(angleRad).toFloat() * length).coerceIn(0f, 1f)
    val endY = (startY + sin(angleRad).toFloat() * length).coerceIn(0f, 1f)

    val segments = mutableListOf<Offset>()
    segments.add(Offset(startX, startY))

    val steps = Random.nextInt(3, 6)
    val stepX = (endX - startX) / steps
    val stepY = (endY - startY) / steps

    var x = startX
    var y = startY
    repeat(steps - 1) {
        x += stepX + (Random.nextFloat() - 0.5f) * 0.05f
        y += stepY + (Random.nextFloat() - 0.5f) * 0.05f
        segments.add(Offset(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f)))
    }
    segments.add(Offset(endX, endY))

    return ElectricLineData(
        startX = startX,
        startY = startY,
        endX = endX,
        endY = endY,
        segments = segments,
        alpha = Random.nextFloat() * 0.4f + 0.6f
    )
}

private fun DrawScope.drawElectricLine(line: ElectricLineData) {
    val scaledSegments = line.segments.map {
        Offset(it.x * size.width, it.y * size.height)
    }

    for (i in 0 until scaledSegments.size - 1) {
        // 글로우
        drawLine(
            color = Color(0xFF00FFFF).copy(alpha = line.alpha * 0.3f),
            start = scaledSegments[i],
            end = scaledSegments[i + 1],
            strokeWidth = 8f,
            cap = StrokeCap.Round
        )
        // 메인
        drawLine(
            color = Color(0xFF00FFFF).copy(alpha = line.alpha),
            start = scaledSegments[i],
            end = scaledSegments[i + 1],
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
        // 밝은 중심
        drawLine(
            color = Color.White.copy(alpha = line.alpha),
            start = scaledSegments[i],
            end = scaledSegments[i + 1],
            strokeWidth = 1.5f,
            cap = StrokeCap.Round
        )
    }
}

// ========== 파워 서지 이펙트 헬퍼 ==========

private data class RayData(
    val angle: Float
)

private fun generateRay(angle: Float): RayData {
    return RayData(angle = angle + (Random.nextFloat() - 0.5f) * 10f)
}

// ========== 썬더볼트 이펙트 헬퍼 ==========

private fun generateThunderBolt(): LightningBoltData {
    val startX = 0.5f + (Random.nextFloat() - 0.5f) * 0.2f
    val points = mutableListOf<Offset>()
    var x = startX
    var y = 0f

    points.add(Offset(x, y))

    // 더 굵고 직선적인 번개
    while (y < 0.7f) {
        x += (Random.nextFloat() - 0.5f) * 0.1f
        y += Random.nextFloat() * 0.12f + 0.1f
        points.add(Offset(x.coerceIn(0.2f, 0.8f), y.coerceIn(0f, 0.7f)))
    }

    return LightningBoltData(points)
}

private fun generateBranchBolt(mainBolt: LightningBoltData): LightningBoltData {
    if (mainBolt.points.size < 2) return LightningBoltData(emptyList())

    val branchIndex = Random.nextInt(1, mainBolt.points.size - 1)
    val branchPoint = mainBolt.points[branchIndex]

    val points = mutableListOf<Offset>()
    var x = branchPoint.x
    var y = branchPoint.y

    points.add(Offset(x, y))

    val direction = if (Random.nextBoolean()) 1f else -1f
    repeat(Random.nextInt(2, 4)) {
        x += direction * (Random.nextFloat() * 0.08f + 0.03f)
        y += Random.nextFloat() * 0.08f + 0.05f
        points.add(Offset(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f)))
    }

    return LightningBoltData(points)
}

private fun DrawScope.drawThunderBolt(bolt: LightningBoltData, color: Color, strokeWidth: Float = 4f) {
    val scaledPoints = bolt.points.map {
        Offset(it.x * size.width, it.y * size.height)
    }

    for (i in 0 until scaledPoints.size - 1) {
        // 외부 글로우
        drawLine(
            color = color.copy(alpha = 0.15f),
            start = scaledPoints[i],
            end = scaledPoints[i + 1],
            strokeWidth = strokeWidth * 4,
            cap = StrokeCap.Round
        )
        // 글로우
        drawLine(
            color = color.copy(alpha = 0.3f),
            start = scaledPoints[i],
            end = scaledPoints[i + 1],
            strokeWidth = strokeWidth * 2,
            cap = StrokeCap.Round
        )
        // 메인
        drawLine(
            color = color.copy(alpha = 0.8f),
            start = scaledPoints[i],
            end = scaledPoints[i + 1],
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        // 밝은 중심
        drawLine(
            color = Color.White.copy(alpha = 0.9f),
            start = scaledPoints[i],
            end = scaledPoints[i + 1],
            strokeWidth = strokeWidth * 0.5f,
            cap = StrokeCap.Round
        )
    }
}

// ========== 오버차지 이펙트 헬퍼 ==========

private data class ExplosionSparkData(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val alpha: Float,
    val color: Color
)

private fun generateExplosionSpark(): ExplosionSparkData {
    val colors = listOf(
        Color(0xFFFF6600),
        Color(0xFFFFAA00),
        Color(0xFFFF0000),
        Color(0xFFFFFF00),
        Color.White
    )
    return ExplosionSparkData(
        angle = Random.nextFloat() * 360f,
        speed = Random.nextFloat() * 0.5f + 0.5f,
        size = Random.nextFloat() * 6f + 2f,
        alpha = Random.nextFloat() * 0.4f + 0.6f,
        color = colors.random()
    )
}
