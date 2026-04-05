package com.elites.fullcharge.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elites.fullcharge.data.AllTimeRecord
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
    isExpanded: Boolean = true,
    onToggle: () -> Unit = {},
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
    val 승급임박 = 승급임박여부(다음계급까지남은시간)

    // 승급 임박 시 색상 변경 (10초 이하)
    val 메인색상 = if (승급임박) StatusGreen else TossBlue
    val 메인색상Dark = if (승급임박) Color(0xFF15803D) else TossBlueDark
    val 메인색상Light = if (승급임박) Color(0xFF4ADE80) else TossBlueLight

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

    // 화살표 회전 애니메이션
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(300),
        label = "arrowRotation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(메인색상.copy(alpha = 0.08f))
    ) {
        // 헤더 (항상 표시, 클릭하면 토글)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (승급임박) "곧 승급!" else "승급",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (승급임박) 메인색상 else TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${다음계급.koreanName}까지 $남은시간포맷",
                        fontSize = 13.sp,
                        color = 메인색상
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "접기" else "펼치기",
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(arrowRotation)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 진행 바 (항상 표시)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(메인색상.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(메인색상Dark, 메인색상, 메인색상Light)
                            )
                        )
                )
            }
        }

        // 상세 내용 (펼쳐졌을 때만)
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 승급 임박 안내 문구
                if (승급임박) {
                    Text(
                        text = "거의 다 됐어요!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = 메인색상
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 시간 표시 (깜빡이는 효과)
                Text(
                    text = 남은시간포맷,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = 메인색상.copy(alpha = pulseAlpha)
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
 * 스와이프 가능한 리더보드 (실시간 + 역대 랭킹)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun 스와이프리더보드(
    사용자들: List<EliteUser>,
    역대기록들: List<AllTimeRecord>,
    현재사용자ID: String,
    onCollapse: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 2 })

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BackgroundGray)
            .padding(16.dp)
    ) {
        // 탭 인디케이터
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(2) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 24.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) TossBlue else DividerGray
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 페이저 콘텐츠
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            when (page) {
                0 -> 실시간랭킹콘텐츠(
                    사용자들 = 사용자들,
                    현재사용자ID = 현재사용자ID
                )
                1 -> 역대랭킹콘텐츠(
                    역대기록들 = 역대기록들,
                    현재사용자ID = 현재사용자ID
                )
            }
        }

        // 접기 버튼
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onCollapse() }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "접기",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "접기",
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
    }
}

/**
 * 실시간 랭킹 콘텐츠
 */
@Composable
private fun 실시간랭킹콘텐츠(
    사용자들: List<EliteUser>,
    현재사용자ID: String
) {
    val 정렬된사용자들 = 사용자들
        .filter { it.isOnline }
        .sortedByDescending { it.sessionDuration }

    Column(modifier = Modifier.fillMaxWidth()) {
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
            정렬된사용자들.take(5).forEachIndexed { index, user ->
                리더보드항목(
                    순위 = index + 1,
                    사용자 = user,
                    본인여부 = user.userId == 현재사용자ID
                )
                if (index < 정렬된사용자들.size - 1 && index < 4) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * 역대 랭킹 콘텐츠
 */
@Composable
private fun 역대랭킹콘텐츠(
    역대기록들: List<AllTimeRecord>,
    현재사용자ID: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "역대 랭킹",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "최장 생존 기록",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (역대기록들.isEmpty()) {
            Text(
                text = "아직 기록이 없어요. 첫 번째 전설이 되어보세요!",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else {
            역대기록들.take(5).forEachIndexed { index, record ->
                역대랭킹항목(
                    순위 = index + 1,
                    기록 = record,
                    본인여부 = record.oderId == 현재사용자ID
                )
                if (index < 역대기록들.size - 1 && index < 4) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * 역대 랭킹 항목
 */
@Composable
private fun 역대랭킹항목(
    순위: Int,
    기록: AllTimeRecord,
    본인여부: Boolean
) {
    val 계급 = 기록.rank
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
        // 순위 (메달 이모지 추가)
        val 순위표시 = when (순위) {
            1 -> "🥇"
            2 -> "🥈"
            3 -> "🥉"
            else -> "$순위"
        }
        Text(
            text = 순위표시,
            fontSize = 16.sp,
            fontWeight = if (순위 <= 3) FontWeight.Bold else FontWeight.Normal,
            color = if (순위 <= 3) TossBlue else TextSecondary,
            modifier = Modifier.width(28.dp)
        )

        // 등급 배지 (버블 색상과 통일)
        val isEnlisted = 계급 in listOf(
            EliteRank.TRAINEE, EliteRank.PRIVATE_SECOND, EliteRank.PRIVATE_FIRST,
            EliteRank.CORPORAL, EliteRank.SERGEANT
        )
        val 배지색상 = when (계급) {
            EliteRank.GENERAL, EliteRank.LIEUTENANT_GENERAL,
            EliteRank.MAJOR_GENERAL, EliteRank.BRIGADIER_GENERAL -> Color(0xFFD97706)
            EliteRank.COLONEL, EliteRank.LIEUTENANT_COLONEL, EliteRank.MAJOR -> Color(0xFF7C3AED)
            EliteRank.CAPTAIN, EliteRank.FIRST_LIEUTENANT, EliteRank.SECOND_LIEUTENANT -> Color(0xFF10B981)
            EliteRank.SERGEANT_MAJOR, EliteRank.MASTER_SERGEANT,
            EliteRank.SERGEANT_FIRST, EliteRank.STAFF_SERGEANT -> Color(0xFF3B82F6)
            else -> Color(0xFFFEE500)  // 병사/훈련병: 노란색
        }
        val 텍스트색상 = if (isEnlisted) Color.Black else Color.White
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(배지색상)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = 계급.koreanName,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = 텍스트색상
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 닉네임
        Text(
            text = if (본인여부) "${기록.nickname} (나)" else 기록.nickname,
            fontSize = 14.sp,
            fontWeight = if (본인여부) FontWeight.Bold else FontWeight.Normal,
            color = if (본인여부) TossBlue else TextPrimary,
            modifier = Modifier.weight(1f)
        )

        // 기록 시간
        Text(
            text = 기록.formattedDuration,
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

/**
 * 실시간 랭킹 (기존 호환용)
 */
@Composable
fun 실시간리더보드(
    사용자들: List<EliteUser>,
    현재사용자ID: String,
    onCollapse: () -> Unit = {},
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
            정렬된사용자들.take(5).forEachIndexed { index, user ->
                리더보드항목(
                    순위 = index + 1,
                    사용자 = user,
                    본인여부 = user.userId == 현재사용자ID
                )
                if (index < 정렬된사용자들.size - 1 && index < 4) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // 접기 버튼
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onCollapse() }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "접기",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "접기",
                fontSize = 13.sp,
                color = TextSecondary
            )
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

        // 등급 배지 (버블 색상과 통일)
        val isEnlisted = 계급 in listOf(
            EliteRank.TRAINEE, EliteRank.PRIVATE_SECOND, EliteRank.PRIVATE_FIRST,
            EliteRank.CORPORAL, EliteRank.SERGEANT
        )
        val 배지색상 = when (계급) {
            // 장성
            EliteRank.GENERAL, EliteRank.LIEUTENANT_GENERAL,
            EliteRank.MAJOR_GENERAL, EliteRank.BRIGADIER_GENERAL -> Color(0xFFD97706)
            // 영관급
            EliteRank.COLONEL, EliteRank.LIEUTENANT_COLONEL, EliteRank.MAJOR -> Color(0xFF7C3AED)
            // 위관급
            EliteRank.CAPTAIN, EliteRank.FIRST_LIEUTENANT, EliteRank.SECOND_LIEUTENANT -> Color(0xFF10B981)
            // 부사관
            EliteRank.SERGEANT_MAJOR, EliteRank.MASTER_SERGEANT,
            EliteRank.SERGEANT_FIRST, EliteRank.STAFF_SERGEANT -> Color(0xFF3B82F6)
            // 병사/훈련병: 노란색
            else -> Color(0xFFFEE500)
        }
        val 텍스트색상 = if (isEnlisted) Color.Black else Color.White
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(배지색상)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = 계급.koreanName,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = 텍스트색상
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
    // 병사/훈련병 여부 확인
    val isEnlisted = 새계급 in listOf(
        EliteRank.TRAINEE, EliteRank.PRIVATE_SECOND, EliteRank.PRIVATE_FIRST,
        EliteRank.CORPORAL, EliteRank.SERGEANT
    )

    // 버블 색상과 통일된 배지 색상 (RankBadge와 동일)
    val badgeColors = when (새계급) {
        // 장성 (골드 그라데이션)
        EliteRank.GENERAL -> listOf(Color(0xFF78350F), Color(0xFFD97706), Color(0xFFFBBF24))
        EliteRank.LIEUTENANT_GENERAL -> listOf(Color(0xFF92400E), Color(0xFFD97706))
        EliteRank.MAJOR_GENERAL -> listOf(Color(0xFFB45309), Color(0xFFD97706))
        EliteRank.BRIGADIER_GENERAL -> listOf(Color(0xFFD97706), Color(0xFFF59E0B))
        // 영관급 (보라색 그라데이션)
        EliteRank.COLONEL -> listOf(Color(0xFF581C87), Color(0xFF7C3AED))
        EliteRank.LIEUTENANT_COLONEL -> listOf(Color(0xFF6D28D9), Color(0xFF8B5CF6))
        EliteRank.MAJOR -> listOf(Color(0xFF7C3AED), Color(0xFFA78BFA))
        // 위관급 (초록색 그라데이션)
        EliteRank.CAPTAIN -> listOf(Color(0xFF047857), Color(0xFF10B981))
        EliteRank.FIRST_LIEUTENANT -> listOf(Color(0xFF059669), Color(0xFF34D399))
        EliteRank.SECOND_LIEUTENANT -> listOf(Color(0xFF10B981), Color(0xFF6EE7B7))
        // 부사관 (파란색 그라데이션)
        EliteRank.SERGEANT_MAJOR -> listOf(Color(0xFF1E40AF), Color(0xFF3B82F6))
        EliteRank.MASTER_SERGEANT -> listOf(Color(0xFF1D4ED8), Color(0xFF60A5FA))
        EliteRank.SERGEANT_FIRST -> listOf(Color(0xFF2563EB), Color(0xFF93C5FD))
        EliteRank.STAFF_SERGEANT -> listOf(Color(0xFF3B82F6), Color(0xFFBFDBFE))
        // 병사/훈련병 (카카오톡 스타일 노란색)
        else -> listOf(Color(0xFFFEE500), Color(0xFFFEE500))
    }

    // 텍스트 색상 (병사/훈련병은 검은색)
    val textColor = if (isEnlisted) Color.Black else Color.White

    // 계급별 아이콘 (RankBadge와 동일)
    val rankIcon = when (새계급) {
        EliteRank.GENERAL -> "⭐"
        EliteRank.LIEUTENANT_GENERAL, EliteRank.MAJOR_GENERAL, EliteRank.BRIGADIER_GENERAL -> "★"
        EliteRank.COLONEL, EliteRank.LIEUTENANT_COLONEL, EliteRank.MAJOR -> "◆"
        EliteRank.CAPTAIN, EliteRank.FIRST_LIEUTENANT, EliteRank.SECOND_LIEUTENANT -> "▲"
        EliteRank.SERGEANT_MAJOR, EliteRank.MASTER_SERGEANT, EliteRank.SERGEANT_FIRST, EliteRank.STAFF_SERGEANT -> "■"
        else -> null
    }

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

                Spacer(modifier = Modifier.height(16.dp))

                // 배지 스타일 계급 표시 (RankBadge와 통일)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.horizontalGradient(badgeColors))
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (rankIcon != null) {
                            Text(
                                text = rankIcon,
                                fontSize = 20.sp,
                                color = textColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = 새계급.koreanName,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
        EliteRank.TRAINEE -> EliteRank.PRIVATE_SECOND
        EliteRank.PRIVATE_SECOND -> EliteRank.PRIVATE_FIRST
        EliteRank.PRIVATE_FIRST -> EliteRank.CORPORAL
        EliteRank.CORPORAL -> EliteRank.SERGEANT
        EliteRank.SERGEANT -> EliteRank.STAFF_SERGEANT
        EliteRank.STAFF_SERGEANT -> EliteRank.SERGEANT_FIRST
        EliteRank.SERGEANT_FIRST -> EliteRank.MASTER_SERGEANT
        EliteRank.MASTER_SERGEANT -> EliteRank.SERGEANT_MAJOR
        EliteRank.SERGEANT_MAJOR -> EliteRank.SECOND_LIEUTENANT
        EliteRank.SECOND_LIEUTENANT -> EliteRank.FIRST_LIEUTENANT
        EliteRank.FIRST_LIEUTENANT -> EliteRank.CAPTAIN
        EliteRank.CAPTAIN -> EliteRank.MAJOR
        EliteRank.MAJOR -> EliteRank.LIEUTENANT_COLONEL
        EliteRank.LIEUTENANT_COLONEL -> EliteRank.COLONEL
        EliteRank.COLONEL -> EliteRank.BRIGADIER_GENERAL
        EliteRank.BRIGADIER_GENERAL -> EliteRank.MAJOR_GENERAL
        EliteRank.MAJOR_GENERAL -> EliteRank.LIEUTENANT_GENERAL
        EliteRank.LIEUTENANT_GENERAL -> EliteRank.GENERAL
        EliteRank.GENERAL -> null
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
        else -> "${초}초"
    }
}

private fun 승급임박여부(밀리초: Long): Boolean {
    return 밀리초 in 1..10000  // 10초 이하
}
