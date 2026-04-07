package com.elites.fullcharge.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elites.fullcharge.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

data class OnboardingPage(
    val emoji: String,
    val title: String,
    val description: String
)

private val onboardingPages = listOf(
    OnboardingPage(
        emoji = "🔋",
        title = "완충 전우회에 오신 걸\n환영합니다",
        description = "배터리 100%인 사람만\n입장할 수 있는 특별한 채팅방이에요"
    ),
    OnboardingPage(
        emoji = "⚡",
        title = "입장 조건",
        description = "배터리가 100%일 때만\n입장 버튼이 활성화돼요\n\n충전 중이든 아니든 상관없어요"
    ),
    OnboardingPage(
        emoji = "⏱️",
        title = "생존 규칙",
        description = "채팅 중 배터리가 99%로 떨어지면\n10초 카운트다운이 시작돼요\n\n10초 안에 충전하지 않으면 퇴장!"
    ),
    OnboardingPage(
        emoji = "🎖️",
        title = "계급 시스템",
        description = "채팅방에 오래 머물수록\n계급이 올라가요"
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    isCharging: Boolean = false,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingPages.lastIndex

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWhite)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // 충전 중일 때 파티클 효과
        if (isCharging) {
            FloatingParticles(
                particleCount = 25,
                speedMultiplier = 0.8f
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 상단 여백 (건너뛰기 버튼 제거됨)
            Spacer(modifier = Modifier.height(48.dp))

            // 페이지 콘텐츠 (스와이프 가능)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(
                    page = onboardingPages[page]
                )
            }

            // 페이지 인디케이터
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 32.dp)
            ) {
                onboardingPages.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (index == pagerState.currentPage) 24.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage) TossBlue
                                else DividerGray
                            )
                    )
                }
            }

            // 버튼 펄스 효과
            val infiniteTransition = rememberInfiniteTransition(label = "buttonPulse")
            val buttonScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.02f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            // 다음/시작하기 버튼
            Button(
                onClick = {
                    if (isLastPage) {
                        onComplete()
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .scale(buttonScale)
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TossBlue
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Text(
                    text = if (isLastPage) "시작하기" else "다음",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 건너뛰기 버튼 (다음 버튼 아래)
            TextButton(
                onClick = onComplete,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "건너뛰기",
                    color = TextTertiary,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 이모지
        Text(
            text = page.emoji,
            fontSize = 80.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 제목
        Text(
            text = page.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 설명
        Text(
            text = page.description,
            fontSize = 16.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

@Composable
private fun FloatingParticles(
    particleCount: Int = 25,
    speedMultiplier: Float = 0.8f
) {
    data class Particle(
        val id: Int,
        var x: Float,
        var y: Float,
        val size: Float,
        val speed: Float,
        val alpha: Float,
        val wobbleOffset: Float,
        val wobbleSpeed: Float
    )

    var particles by remember(particleCount) {
        mutableStateOf(
            List(particleCount) { index ->
                Particle(
                    id = index,
                    x = Random.nextFloat(),
                    y = Random.nextFloat(),
                    size = Random.nextFloat() * 5f + 2f,
                    speed = (Random.nextFloat() * 0.0015f + 0.0008f) * speedMultiplier,
                    alpha = Random.nextFloat() * 0.35f + 0.1f,
                    wobbleOffset = Random.nextFloat() * 100f,
                    wobbleSpeed = Random.nextFloat() * 0.04f + 0.02f
                )
            }
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    LaunchedEffect(time) {
        particles = particles.map { p ->
            val newY = p.y - p.speed
            val wobble = sin((time + p.wobbleOffset) * p.wobbleSpeed) * 0.0015f
            val newX = p.x + wobble

            if (newY < -0.05f) {
                p.copy(
                    x = Random.nextFloat(),
                    y = 1.05f,
                    alpha = Random.nextFloat() * 0.35f + 0.1f
                )
            } else {
                p.copy(x = newX.coerceIn(0f, 1f), y = newY)
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            drawCircle(
                color = TossBlue.copy(alpha = p.alpha * 0.3f),
                radius = p.size * 2.5f,
                center = Offset(p.x * size.width, p.y * size.height)
            )
            drawCircle(
                color = TossBlue.copy(alpha = p.alpha),
                radius = p.size,
                center = Offset(p.x * size.width, p.y * size.height)
            )
            drawCircle(
                color = Color.White.copy(alpha = p.alpha * 0.6f),
                radius = p.size * 0.35f,
                center = Offset(p.x * size.width, p.y * size.height)
            )
        }
    }
}
