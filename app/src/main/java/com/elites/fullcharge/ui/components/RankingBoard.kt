package com.elites.fullcharge.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elites.fullcharge.data.AllTimeRecord
import com.elites.fullcharge.data.EliteRank
import com.elites.fullcharge.data.EliteUser
import com.elites.fullcharge.ui.components.RankInsignia
import com.elites.fullcharge.ui.theme.*

/**
 * v0 스타일 랭킹 보드 컴포넌트
 * - 실시간/역대 랭킹 탭
 * - 스와이프 제스처 지원
 * - 메달 아이콘 (🥇🥈🥉)
 */

enum class RankingTab {
    LIVE, ALL_TIME
}

@Composable
fun TacticalRankingBoard(
    liveRanking: List<EliteUser>,
    allTimeRanking: List<AllTimeRecord>,
    currentUserId: String,
    modifier: Modifier = Modifier,
    onCollapse: () -> Unit = {}
) {
    var activeTab by remember { mutableStateOf(RankingTab.LIVE) }
    var dragOffset by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBlack.copy(alpha = 0.8f))
            .border(
                width = 1.dp,
                color = BorderMuted.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragOffset > 50 && activeTab == RankingTab.ALL_TIME) {
                            activeTab = RankingTab.LIVE
                        } else if (dragOffset < -50 && activeTab == RankingTab.LIVE) {
                            activeTab = RankingTab.ALL_TIME
                        }
                        dragOffset = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        dragOffset += dragAmount
                    }
                )
            }
    ) {
        Column {
            // 탭 헤더
            RankingTabHeader(
                activeTab = activeTab,
                onTabChange = { activeTab = it }
            )

            // 스와이프 인디케이터
            SwipeIndicator(activeTab = activeTab)

            // 랭킹 리스트
            when (activeTab) {
                RankingTab.LIVE -> {
                    LiveRankingList(
                        users = liveRanking,
                        currentUserId = currentUserId
                    )
                }
                RankingTab.ALL_TIME -> {
                    AllTimeRankingList(
                        records = allTimeRanking,
                        currentUserId = currentUserId
                    )
                }
            }

            // 푸터 힌트
            RankingFooter(activeTab = activeTab)
        }
    }
}

@Composable
private fun RankingTabHeader(
    activeTab: RankingTab,
    onTabChange: (RankingTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = BorderMuted.copy(alpha = 0.3f),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            )
    ) {
        // 실시간 랭킹 탭
        TabButton(
            text = "실시간 랭킹",
            icon = "⏱️",
            isActive = activeTab == RankingTab.LIVE,
            onClick = { onTabChange(RankingTab.LIVE) },
            modifier = Modifier.weight(1f)
        )

        // 역대 랭킹 탭
        TabButton(
            text = "역대 랭킹",
            icon = "🏆",
            isActive = activeTab == RankingTab.ALL_TIME,
            onClick = { onTabChange(RankingTab.ALL_TIME) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TabButton(
    text: String,
    icon: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) EliteGreen.copy(alpha = 0.1f) else CardBlack,
        label = "tab_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isActive) EliteGreen else ForegroundMuted,
        label = "tab_text"
    )

    Box(
        modifier = modifier
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = icon, fontSize = 12.sp)
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }

        // 활성 탭 하단 인디케이터
        if (isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(EliteGreen)
            )
        }
    }
}

@Composable
private fun SwipeIndicator(activeTab: RankingTab) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(
                width = 1.dp,
                color = BorderMuted.copy(alpha = 0.3f),
                shape = RoundedCornerShape(0.dp)
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowLeft,
            contentDescription = null,
            tint = if (activeTab == RankingTab.LIVE) ForegroundMuted.copy(alpha = 0.2f) else EliteGreen.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 도트 인디케이터
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (activeTab == RankingTab.LIVE) EliteGreen else ForegroundMuted)
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (activeTab == RankingTab.ALL_TIME) EliteGreen else ForegroundMuted)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = if (activeTab == RankingTab.ALL_TIME) ForegroundMuted.copy(alpha = 0.2f) else EliteGreen.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun LiveRankingList(
    users: List<EliteUser>,
    currentUserId: String
) {
    if (users.isEmpty()) {
        EmptyRankingMessage(message = "랭킹 데이터가 없습니다")
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(192.dp)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(users.take(10)) { index, user ->
                RankingItem(
                    position = index,
                    nickname = user.nickname,
                    rank = user.rank,
                    rankName = user.rank.koreanName,
                    duration = user.formattedDuration,
                    isMe = user.userId == currentUserId,
                    isTopThree = index < 3
                )
            }
        }
    }
}

@Composable
private fun AllTimeRankingList(
    records: List<AllTimeRecord>,
    currentUserId: String
) {
    if (records.isEmpty()) {
        EmptyRankingMessage(message = "랭킹 데이터가 없습니다")
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(192.dp)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(records.take(10)) { index, record ->
                RankingItem(
                    position = index,
                    nickname = record.nickname,
                    rank = record.rank,
                    rankName = record.rank.koreanName,
                    duration = record.formattedDuration,
                    isMe = record.oderId == currentUserId,
                    isTopThree = index < 3,
                    showBestRecord = true
                )
            }
        }
    }
}

@Composable
private fun RankingItem(
    position: Int,
    nickname: String,
    rank: EliteRank,
    rankName: String,
    duration: String,
    isMe: Boolean,
    isTopThree: Boolean,
    showBestRecord: Boolean = false
) {
    val backgroundColor = when {
        isMe -> EliteGreen.copy(alpha = 0.15f)
        isTopThree -> MutedBlack.copy(alpha = 0.3f)
        else -> CardBlack
    }

    val borderColor = if (isMe) EliteGreen.copy(alpha = 0.3f) else CardBlack

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .then(
                if (isMe) Modifier.border(1.dp, borderColor, RoundedCornerShape(8.dp))
                else Modifier
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 순위 (메달 또는 숫자)
        Box(
            modifier = Modifier.width(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = getMedalIcon(position),
                fontSize = if (position < 3) 16.sp else 12.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 계급 아이콘
        RankInsignia(rank = rank, size = 18.dp)

        Spacer(modifier = Modifier.width(8.dp))

        // 유저 정보
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isMe) "나" else nickname,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isMe) EliteGreen else ForegroundWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = rankName,
                fontSize = 11.sp,
                color = ForegroundMuted
            )
        }

        // 생존 시간
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = duration,
                style = MonoTypography.hudSmall,
                color = if (isMe) EliteGreen else ForegroundWhite
            )
            if (showBestRecord) {
                Text(
                    text = "최고 기록",
                    fontSize = 10.sp,
                    color = ForegroundMuted
                )
            }
        }
    }
}

@Composable
private fun EmptyRankingMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            fontSize = 14.sp,
            color = ForegroundMuted
        )
    }
}

@Composable
private fun RankingFooter(activeTab: RankingTab) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MutedBlack.copy(alpha = 0.2f))
            .border(
                width = 1.dp,
                color = BorderMuted.copy(alpha = 0.3f),
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
            )
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when (activeTab) {
                RankingTab.LIVE -> "현재 접속 중인 전우들의 실시간 순위"
                RankingTab.ALL_TIME -> "완충 전우회 역대 명예의 전당"
            },
            fontSize = 11.sp,
            color = ForegroundMuted
        )
    }
}

private fun getMedalIcon(position: Int): String {
    return when (position) {
        0 -> "🥇"
        1 -> "🥈"
        2 -> "🥉"
        else -> "${position + 1}"
    }
}
