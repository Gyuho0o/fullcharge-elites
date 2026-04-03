package com.elites.fullcharge.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elites.fullcharge.data.EliteRank
import com.elites.fullcharge.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun ExileScreen(
    sessionDuration: Long,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showContent by remember { mutableStateOf(false) }
    val formattedDuration = EliteRank.fromDurationFormatted(sessionDuration)
    val finalRank = EliteRank.fromDuration(sessionDuration)

    // 등장 애니메이션
    LaunchedEffect(Unit) {
        showContent = true
        delay(3500)
        onDismiss()
    }

    // 페이드 인
    val alpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(400),
        label = "alpha"
    )

    // 스케일 애니메이션
    val scale by animateFloatAsState(
        targetValue = if (showContent) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWhite)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentAlignment = Alignment.Center
    ) {
        // 떨어지는 파티클 효과
        FallingParticles()

        Column(
            modifier = Modifier
                .alpha(alpha)
                .scale(scale)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 메인 메시지
            Text(
                text = "퇴장되었어요",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "시간 내에 충전하지 못했어요",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 세션 정보 카드
            SessionSummaryCard(
                duration = formattedDuration,
                rank = finalRank
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 안내 메시지 - 재입장 안내
            Text(
                text = "다시 100%로 충전하면\n재도전할 수 있어요",
                fontSize = 14.sp,
                color = TextTertiary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 자동 이동 카운트다운
            AutoDismissIndicator()
        }
    }
}

@Composable
private fun FallingParticles() {
    data class Particle(
        val id: Int,
        var x: Float,
        var y: Float,
        val size: Float,
        val speed: Float,
        val alpha: Float
    )

    var particles by remember {
        mutableStateOf(
            List(12) { index ->
                Particle(
                    id = index,
                    x = Random.nextFloat(),
                    y = Random.nextFloat() - 0.5f,
                    size = Random.nextFloat() * 4f + 2f,
                    speed = Random.nextFloat() * 0.002f + 0.001f,
                    alpha = Random.nextFloat() * 0.2f + 0.05f
                )
            }
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    LaunchedEffect(time) {
        particles = particles.map { p ->
            val newY = p.y + p.speed
            if (newY > 1.1f) {
                p.copy(x = Random.nextFloat(), y = -0.1f)
            } else {
                p.copy(y = newY)
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            drawCircle(
                color = TextTertiary.copy(alpha = p.alpha),
                radius = p.size,
                center = Offset(p.x * size.width, p.y * size.height)
            )
        }
    }
}

@Composable
private fun SessionSummaryCard(
    duration: String,
    rank: EliteRank
) {
    // 숫자 카운트업 애니메이션 효과
    val infiniteTransition = rememberInfiniteTransition(label = "card")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BackgroundGray,
                        TossBlue.copy(alpha = 0.05f)
                    )
                )
            )
            .padding(32.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "이번 체류 기록",
                fontSize = 14.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 시간 표시 (강조)
            Text(
                text = duration,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = TossBlue
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 등급 배지
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                TossBlueDark.copy(alpha = glowAlpha),
                                TossBlue.copy(alpha = glowAlpha),
                                TossBlueLight.copy(alpha = glowAlpha)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = rank.koreanName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = rank.description,
                fontSize = 12.sp,
                color = TextTertiary
            )
        }
    }
}

@Composable
private fun AutoDismissIndicator() {
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        val duration = 3500L

        while (progress < 1f) {
            val elapsed = System.currentTimeMillis() - startTime
            progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
            delay(16)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 진행 바
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(DividerGray)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(TossBlue)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "자동으로 돌아갑니다",
            fontSize = 12.sp,
            color = TextTertiary
        )
    }
}
