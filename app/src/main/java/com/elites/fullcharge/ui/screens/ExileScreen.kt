package com.elites.fullcharge.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elites.fullcharge.data.EliteRank
import com.elites.fullcharge.ui.components.AppLogo
import com.elites.fullcharge.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.random.Random

// ============================================================
// ExileScreen - v0 Tactical Military Redesign (Dishonorable Discharge)
// ============================================================

@Composable
fun ExileScreen(
    sessionDuration: Long,
    currentBatteryLevel: Int = 0,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showContent by remember { mutableStateOf(false) }
    val formattedDuration = EliteRank.fromDurationFormatted(sessionDuration)
    val finalRank = EliteRank.fromDuration(sessionDuration)
    val nextRank = EliteRank.entries.getOrNull(finalRank.ordinal + 1)
    val timeToNextRank = nextRank?.let {
        val nextRankMinMillis = it.minMinutes * 60 * 1000L
        val remaining = nextRankMinMillis - sessionDuration
        if (remaining > 0) EliteRank.fromDurationFormatted(remaining) else null
    }

    // 등장 애니메이션
    LaunchedEffect(Unit) {
        showContent = true
        delay(4000)
        onDismiss()
    }

    // 페이드 인
    val alpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(500),
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
            .background(BackgroundBlack)
    ) {
        // 배경 그리드 패턴
        TacticalGridBackground()

        // 떨어지는 파티클 효과
        FallingParticles()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // ===== HEADER =====
            DischargeHeader()

            // ===== MAIN CONTENT =====
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .alpha(alpha)
                    .scale(scale)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Skull 아이콘 (레드 글로우)
                SkullIcon()

                Spacer(modifier = Modifier.height(24.dp))

                // 타이틀
                Text(
                    text = "불명예 퇴장",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = CrisisRed
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "EXILED FROM THE ELITE",
                    style = MonoTypography.subtitle,
                    color = CrisisRed.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Mission Report 카드
                MissionReportCard(
                    rank = finalRank,
                    duration = formattedDuration,
                    timeToNextRank = timeToNextRank,
                    nextRank = nextRank
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 귀책 문구
                Text(
                    text = "귀하는 전우들과의 약속을 저버렸습니다",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ForegroundMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 현재 배터리 상태 (재입대 준비)
                BatteryStatusBadge(level = currentBatteryLevel)

                Spacer(modifier = Modifier.height(24.dp))

                // 재입대 버튼
                ReEnlistButton(
                    isEnabled = currentBatteryLevel >= 100,
                    onDismiss = onDismiss
                )
            }

            // ===== FOOTER =====
            DischargeFooter()
        }
    }
}

// ===== HEADER =====
@Composable
private fun DischargeHeader() {
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
                        color = CrisisRed
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "불명예 제대 | DISHONORABLE DISCHARGE",
                        style = MonoTypography.tracking,
                        color = CrisisRed
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(CrisisRed.copy(alpha = 0.3f))
            )
        }
    }
}

// ===== SKULL ICON =====
@Composable
private fun SkullIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "skull_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        contentAlignment = Alignment.Center
    ) {
        // 글로우 효과
        Canvas(modifier = Modifier.size(100.dp)) {
            drawCircle(
                color = CrisisRed.copy(alpha = glowAlpha * 0.5f),
                radius = size.minDimension / 2
            )
        }

        // Skull 이모지
        Text(
            text = "💀",
            fontSize = 64.sp
        )
    }
}

// ===== MISSION REPORT CARD =====
@Composable
private fun MissionReportCard(
    rank: EliteRank,
    duration: String,
    timeToNextRank: String?,
    nextRank: EliteRank?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = BorderMuted,
                shape = RoundedCornerShape(12.dp)
            ),
        color = CardBlack,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // 헤더
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MutedBlack)
                    .padding(12.dp)
            ) {
                Text(
                    text = "임무 보고서 | MISSION REPORT",
                    style = MonoTypography.hudMedium,
                    color = ForegroundMuted
                )
            }

            // 내용
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // 최종 계급
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🎖️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "최종 계급",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ForegroundMuted
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MutedBlack
                    ) {
                        Text(
                            text = rank.koreanName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = ForegroundWhite,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 생존 시간
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⏰", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "생존 시간",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ForegroundMuted
                        )
                    }
                    Text(
                        text = duration,
                        style = MonoTypography.hudLarge,
                        color = ForegroundWhite
                    )
                }

                // 다음 계급까지 남은 시간
                if (timeToNextRank != null && nextRank != null) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = CrisisRed.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = CrisisRed.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "다음 계급 ${nextRank.koreanName}까지 $timeToNextRank 남음",
                            style = MaterialTheme.typography.bodySmall,
                            color = CrisisRed,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// ===== BATTERY STATUS BADGE =====
@Composable
private fun BatteryStatusBadge(level: Int) {
    val color = if (level >= 100) EliteGreen else ForegroundMuted

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f),
        modifier = Modifier.border(
            width = 1.dp,
            color = color.copy(alpha = 0.3f),
            shape = RoundedCornerShape(8.dp)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🔋", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "현재 배터리: $level%",
                style = MonoTypography.hudMedium,
                color = color
            )
        }
    }
}

// ===== RE-ENLIST BUTTON =====
@Composable
private fun ReEnlistButton(
    isEnabled: Boolean,
    onDismiss: () -> Unit
) {
    var progress by remember { mutableFloatStateOf(0f) }

    // 자동 이동 타이머
    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        val duration = 4000L

        while (progress < 1f) {
            val elapsed = System.currentTimeMillis() - startTime
            progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
            delay(16)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isEnabled) EliteGreen else MutedBlack,
                contentColor = if (isEnabled) BackgroundBlack else ForegroundMuted
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = if (isEnabled) "재입대 준비 완료" else "재입대 준비 (완충 시 활성화)",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isEnabled) FontWeight.Bold else FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 자동 이동 프로그레스
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MutedBlack)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(ForegroundMuted)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "자동으로 돌아갑니다",
            style = MonoTypography.hudSmall,
            color = ForegroundDim
        )
    }
}

// ===== FOOTER =====
@Composable
private fun DischargeFooter() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(BorderMuted.copy(alpha = 0.3f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\"패배는 일시적이다. 포기만이 영원하다.\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = ForegroundDim,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ===== BACKGROUND EFFECTS =====
@Composable
private fun TacticalGridBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSize = 20.dp.toPx()
        val lineColor = CrisisRed.copy(alpha = 0.02f)

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
            List(15) { index ->
                Particle(
                    id = index,
                    x = Random.nextFloat(),
                    y = Random.nextFloat() - 0.5f,
                    size = Random.nextFloat() * 3f + 1f,
                    speed = Random.nextFloat() * 0.002f + 0.001f,
                    alpha = Random.nextFloat() * 0.3f + 0.1f
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
            // 글로우
            drawCircle(
                color = CrisisRed.copy(alpha = p.alpha * 0.3f),
                radius = p.size * 2f,
                center = Offset(p.x * size.width, p.y * size.height)
            )
            // 코어
            drawCircle(
                color = CrisisRed.copy(alpha = p.alpha),
                radius = p.size,
                center = Offset(p.x * size.width, p.y * size.height)
            )
        }
    }
}
