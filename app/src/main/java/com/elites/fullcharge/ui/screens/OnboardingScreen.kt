package com.elites.fullcharge.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elites.fullcharge.ui.components.AppLogo
import com.elites.fullcharge.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

// ============================================================
// OnboardingScreen - v0 Tactical Military Redesign (Boot Camp)
// ============================================================

data class OnboardingPage(
    val emoji: String,
    val rule: String,           // RULE #1, RULE #2, ...
    val ruleEnglish: String,    // 영어 서브타이틀
    val title: String,          // 한글 타이틀
    val description: String,
    val highlight: String?,     // 하이라이트 배지 (예: "100%")
    val borderColor: Color      // 아이콘 박스 테두리 색상
)

private val onboardingPages = listOf(
    OnboardingPage(
        emoji = "🔋",
        rule = "RULE #1",
        ruleEnglish = "FULL CHARGE ONLY",
        title = "100% 완충만이 살 길이다",
        description = "완충 전우회는 오직 배터리 100%인\n전우만 입장할 수 있습니다.\n\n충전기를 연결하고 100%까지 충전하십시오.",
        highlight = "100%",
        borderColor = EliteGreen
    ),
    OnboardingPage(
        emoji = "⚡",
        rule = "RULE #2",
        ruleEnglish = "ENTRY CLEARANCE",
        title = "입장 허가 조건",
        description = "배터리가 정확히 100%일 때만\n입장 버튼이 활성화됩니다.\n\n충전 중이든 아니든 상관없이\n100%만 채우면 입장 허가!",
        highlight = null,
        borderColor = WarningAmber
    ),
    OnboardingPage(
        emoji = "⏱️",
        rule = "RULE #3",
        ruleEnglish = "SURVIVAL PROTOCOL",
        title = "10초 생존 규칙",
        description = "채팅 중 배터리가 99%로 떨어지면\n10초 카운트다운이 시작됩니다.\n\n10초 내에 충전기를 연결하지 않으면\n불명예 퇴장 처리됩니다!",
        highlight = "10초",
        borderColor = CrisisRed
    ),
    OnboardingPage(
        emoji = "🎖️",
        rule = "RULE #4",
        ruleEnglish = "RANK SYSTEM",
        title = "계급 상승 시스템",
        description = "채팅방에 오래 머물수록\n계급이 올라갑니다.\n\n훈련병부터 시작하여\n대장까지 21단계 계급이 있습니다.",
        highlight = "21단계",
        borderColor = RankOfficer
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    isCharging: Boolean = false,
    isElite: Boolean = false,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingPages.lastIndex
    val isFirstPage = pagerState.currentPage == 0

    // 100%이거나 충전 중이면 효과 표시
    val showEffects = isElite || isCharging
    val progress = (pagerState.currentPage + 1) / onboardingPages.size.toFloat()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundBlack)
    ) {
        // 배경 그리드 패턴
        TacticalGridBackground()

        // 파티클 효과
        if (showEffects) {
            TacticalParticles(
                particleCount = if (isElite) 40 else 20,
                color = EliteGreen
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // ===== HEADER =====
            BootCampHeader()

            // ===== PROGRESS BAR =====
            ProgressSection(
                currentStep = pagerState.currentPage + 1,
                totalSteps = onboardingPages.size,
                progress = progress
            )

            // ===== PAGE CONTENT =====
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(
                    page = onboardingPages[page]
                )
            }

            // ===== NAVIGATION BUTTONS =====
            NavigationSection(
                isFirstPage = isFirstPage,
                isLastPage = isLastPage,
                onPrevious = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                },
                onNext = {
                    if (isLastPage) {
                        onComplete()
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                onSkip = onComplete
            )
        }
    }
}

// ===== HEADER =====
@Composable
private fun BootCampHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    AppLogo(
                        size = 20.dp,
                        animated = false,
                        showGlow = false,
                        color = EliteGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "신병 교육대 | BOOT CAMP",
                        style = MonoTypography.tracking,
                        color = ForegroundMuted
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(BorderMuted.copy(alpha = 0.5f))
            )
        }
    }
}

// ===== PROGRESS SECTION =====
@Composable
private fun ProgressSection(
    currentStep: Int,
    totalSteps: Int,
    progress: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // 프로그레스 바
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MutedBlack)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(EliteGreen)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 진행 상황 텍스트
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "STEP $currentStep/$totalSteps",
                style = MonoTypography.hudMedium,
                color = EliteGreen
            )
            Text(
                text = "${(progress * 100).toInt()}% 완료",
                style = MonoTypography.hudMedium,
                color = ForegroundMuted
            )
        }
    }
}

// ===== PAGE CONTENT =====
@Composable
private fun OnboardingPageContent(
    page: OnboardingPage
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 아이콘 박스 (색상별 테두리)
        Box(
            modifier = Modifier
                .size(120.dp)
                .border(
                    width = 2.dp,
                    color = page.borderColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .background(
                    color = page.borderColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = page.emoji,
                fontSize = 56.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 규칙 번호 (서브타이틀, 영어, 모노폰트)
        Text(
            text = "${page.rule}: ${page.ruleEnglish}",
            style = MonoTypography.subtitle,
            color = page.borderColor.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 타이틀 (한글, 굵은 글씨)
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = ForegroundWhite,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 설명
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyMedium,
            color = ForegroundMuted,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        // 하이라이트 배지
        page.highlight?.let { highlight ->
            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = page.borderColor.copy(alpha = 0.15f),
                modifier = Modifier.border(
                    width = 1.dp,
                    color = page.borderColor,
                    shape = RoundedCornerShape(8.dp)
                )
            ) {
                Text(
                    text = highlight,
                    style = MonoTypography.hudLarge,
                    color = page.borderColor,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }
    }
}

// ===== NAVIGATION SECTION =====
@Composable
private fun NavigationSection(
    isFirstPage: Boolean,
    isLastPage: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 메인 버튼 (이전/다음)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 이전 버튼
            if (!isFirstPage) {
                OutlinedButton(
                    onClick = onPrevious,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ForegroundMuted
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderMuted),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("이전", style = MaterialTheme.typography.bodyLarge)
                }
            }

            // 다음/시작 버튼
            Button(
                onClick = onNext,
                modifier = Modifier
                    .weight(if (isFirstPage) 2f else 1f)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EliteGreen
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isLastPage) "교육 완료" else "다음",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = BackgroundBlack
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 건너뛰기 버튼
        TextButton(onClick = onSkip) {
            Text(
                text = "교육 건너뛰기",
                style = MaterialTheme.typography.bodySmall,
                color = ForegroundDim
            )
        }
    }
}

// ===== BACKGROUND EFFECTS =====
@Composable
private fun TacticalGridBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSize = 20.dp.toPx()
        val lineColor = EliteGreen.copy(alpha = 0.03f)

        var x = 0f
        while (x < size.width) {
            drawLine(
                color = lineColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
            x += gridSize
        }

        var y = 0f
        while (y < size.height) {
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += gridSize
        }
    }
}

@Composable
private fun TacticalParticles(
    particleCount: Int = 30,
    color: Color = EliteGreen
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

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            drawCircle(
                color = color.copy(alpha = p.alpha * 0.3f),
                radius = p.size * 2f,
                center = Offset(p.x * size.width, p.y * size.height)
            )
            drawCircle(
                color = color.copy(alpha = p.alpha),
                radius = p.size,
                center = Offset(p.x * size.width, p.y * size.height)
            )
        }
    }
}
