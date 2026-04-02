package com.elites.fullcharge.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elites.fullcharge.data.EliteRank
import com.elites.fullcharge.data.EliteUser
import com.elites.fullcharge.ui.theme.*
import java.util.concurrent.TimeUnit

/**
 * 다음 등급까지 남은 시간
 */
@Composable
fun 진급카운트다운(
    현재시간: Long,
    modifier: Modifier = Modifier
) {
    val 현재계급 = EliteRank.fromDuration(현재시간)
    val 다음계급 = 다음계급가져오기(현재계급)

    if (다음계급 == null) {
        최고등급배너(modifier)
        return
    }

    val 다음계급까지남은시간 = (다음계급.minMinutes * 60 * 1000) - 현재시간
    val 남은시간포맷 = 시간포맷(다음계급까지남은시간)

    // 진행률 계산
    val 현재계급시작 = 현재계급.minMinutes * 60 * 1000
    val 다음계급시작 = 다음계급.minMinutes * 60 * 1000
    val 진행률 = ((현재시간 - 현재계급시작).toFloat() / (다음계급시작 - 현재계급시작)).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = 진행률,
        animationSpec = tween(500),
        label = "progress"
    )

    // 펄스 효과
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TossBlue.copy(alpha = 0.08f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${다음계급.koreanName} 승급까지",
                fontSize = 14.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))

            // 시간 표시 (깜빡이는 효과)
            Text(
                text = 남은시간포맷,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TossBlue.copy(alpha = pulseAlpha)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 진행 바
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(TossBlue.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(TossBlueDark, TossBlue, TossBlueLight)
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun 최고등급배너(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TossBlue)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "더 이상 올라갈 곳이 없어요",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "당신이 바로 전설입니다",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * 실시간 랭킹
 */
@Composable
fun 실시간리더보드(
    사용자들: List<EliteUser>,
    현재사용자ID: String,
    modifier: Modifier = Modifier
) {
    val 정렬된사용자들 = 사용자들
        .filter { it.isOnline }
        .sortedByDescending { it.sessionDuration }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BackgroundGray)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "실시간 랭킹",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "${정렬된사용자들.size}명 접속 중",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (정렬된사용자들.isEmpty()) {
            Text(
                text = "조용하네요. 당신이 첫 번째예요",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else {
            정렬된사용자들.take(10).forEachIndexed { index, user ->
                리더보드항목(
                    순위 = index + 1,
                    사용자 = user,
                    본인여부 = user.userId == 현재사용자ID
                )
                if (index < 정렬된사용자들.size - 1 && index < 9) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun 리더보드항목(
    순위: Int,
    사용자: EliteUser,
    본인여부: Boolean
) {
    val 계급 = EliteRank.fromDuration(사용자.sessionDuration)
    val 배경색 = when {
        본인여부 -> TossBlue.copy(alpha = 0.1f)
        순위 <= 3 -> BackgroundGray
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(배경색)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 순위
        Text(
            text = "$순위",
            fontSize = 16.sp,
            fontWeight = if (순위 <= 3) FontWeight.Bold else FontWeight.Normal,
            color = if (순위 <= 3) TossBlue else TextSecondary,
            modifier = Modifier.width(28.dp)
        )

        // 등급 배지
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(
                    when (계급) {
                        EliteRank.GOD -> TossBlue
                        EliteRank.SERGEANT -> TossBlueDark
                        EliteRank.PRIVATE -> TossBlueLight.copy(alpha = 0.5f)
                        else -> TextTertiary
                    }
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = 계급.koreanName,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 닉네임
        Text(
            text = if (본인여부) "${사용자.nickname} (나)" else 사용자.nickname,
            fontSize = 14.sp,
            fontWeight = if (본인여부) FontWeight.Bold else FontWeight.Normal,
            color = if (본인여부) TossBlue else TextPrimary,
            modifier = Modifier.weight(1f)
        )

        // 유지 시간
        Text(
            text = EliteRank.fromDurationFormatted(사용자.sessionDuration),
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

/**
 * 등급 상승 알림
 */
@Composable
fun 진급축하오버레이(
    새계급: EliteRank,
    표시: Boolean,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = 표시,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(40.dp)
            ) {
                Text(
                    text = "축하합니다!",
                    fontSize = 16.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = 새계급.koreanName,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = TossBlue
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = 새계급.description,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        }

        LaunchedEffect(표시) {
            if (표시) {
                kotlinx.coroutines.delay(2500)
                onDismiss()
            }
        }
    }
}

// 유틸리티 함수들
private fun 다음계급가져오기(현재계급: EliteRank): EliteRank? {
    return when (현재계급) {
        EliteRank.NEWBIE -> EliteRank.PRIVATE
        EliteRank.PRIVATE -> EliteRank.SERGEANT
        EliteRank.SERGEANT -> EliteRank.GOD
        EliteRank.GOD -> null
    }
}

private fun 시간포맷(밀리초: Long): String {
    if (밀리초 <= 0) return "지금!"

    val 총초 = 밀리초 / 1000
    val 시간 = 총초 / 3600
    val 분 = (총초 % 3600) / 60
    val 초 = 총초 % 60

    return when {
        시간 > 0 -> "${시간}시간 ${분}분 ${초}초"
        분 > 0 -> "${분}분 ${초}초"
        초 <= 10 -> "거의 다 됐어요!"
        else -> "${초}초"
    }
}
