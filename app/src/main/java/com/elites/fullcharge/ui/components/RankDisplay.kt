package com.elites.fullcharge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elites.fullcharge.data.EliteRank
import com.elites.fullcharge.ui.components.RankInsignia
import com.elites.fullcharge.ui.theme.*

/**
 * v0 스타일 계급 표시 컴포넌트
 * 계급 카테고리별 그룹화 (병, 부사관, 위관, 영관/장성)
 */

data class RankCategory(
    val koreanName: String,
    val englishName: String,
    val ranks: List<EliteRank>
)

private val rankCategories = listOf(
    RankCategory(
        koreanName = "병",
        englishName = "ENLISTED",
        ranks = listOf(
            EliteRank.TRAINEE,
            EliteRank.PRIVATE_SECOND,
            EliteRank.PRIVATE_FIRST,
            EliteRank.CORPORAL,
            EliteRank.SERGEANT
        )
    ),
    RankCategory(
        koreanName = "부사관",
        englishName = "NCO",
        ranks = listOf(
            EliteRank.STAFF_SERGEANT,
            EliteRank.SERGEANT_FIRST,
            EliteRank.MASTER_SERGEANT,
            EliteRank.SERGEANT_MAJOR
        )
    ),
    RankCategory(
        koreanName = "위관",
        englishName = "OFFICER",
        ranks = listOf(
            EliteRank.SECOND_LIEUTENANT,
            EliteRank.FIRST_LIEUTENANT,
            EliteRank.CAPTAIN
        )
    ),
    RankCategory(
        koreanName = "영관/장성",
        englishName = "GENERAL",
        ranks = listOf(
            EliteRank.MAJOR,
            EliteRank.LIEUTENANT_COLONEL,
            EliteRank.COLONEL,
            EliteRank.BRIGADIER_GENERAL,
            EliteRank.MAJOR_GENERAL,
            EliteRank.LIEUTENANT_GENERAL,
            EliteRank.GENERAL
        )
    )
)

@Composable
fun TacticalRankDisplay(
    currentRank: EliteRank,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(rankCategories) { category ->
            RankCategorySection(
                category = category,
                currentRank = currentRank
            )
        }
    }
}

@Composable
private fun RankCategorySection(
    category: RankCategory,
    currentRank: EliteRank
) {
    Column {
        // 카테고리 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(BorderMuted.copy(alpha = 0.5f))
            )
            Text(
                text = "${category.koreanName} | ${category.englishName}",
                style = MonoTypography.hudSmall,
                color = ForegroundMuted,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(BorderMuted.copy(alpha = 0.5f))
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 계급 그리드 (2열)
        val chunkedRanks = category.ranks.chunked(2)
        chunkedRanks.forEach { rowRanks ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowRanks.forEach { rank ->
                    RankCard(
                        rank = rank,
                        isCurrent = rank == currentRank,
                        isAchieved = currentRank.ordinal >= rank.ordinal,
                        modifier = Modifier.weight(1f)
                    )
                }
                // 홀수 개인 경우 빈 공간
                if (rowRanks.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RankCard(
    rank: EliteRank,
    isCurrent: Boolean,
    isAchieved: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isCurrent -> EliteGreen.copy(alpha = 0.1f)
        isAchieved -> CardBlack.copy(alpha = 0.5f)
        else -> MutedBlack.copy(alpha = 0.3f)
    }

    val borderColor = when {
        isCurrent -> EliteGreen
        isAchieved -> BorderMuted.copy(alpha = 0.5f)
        else -> BorderMuted.copy(alpha = 0.3f)
    }

    val textColor = when {
        isCurrent -> EliteGreen
        isAchieved -> ForegroundWhite
        else -> ForegroundMuted.copy(alpha = 0.5f)
    }

    val alpha = if (isAchieved) 1f else 0.5f

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = if (isCurrent) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.alpha(alpha)) {
                    RankInsignia(rank = rank, size = 20.dp)
                }
                Text(
                    text = rank.koreanName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = textColor
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatRankDuration(rank),
                style = MonoTypography.hudSmall,
                color = ForegroundMuted.copy(alpha = alpha)
            )
        }

        // 현재 계급 표시
        if (isCurrent) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(EliteGreen)
            )
        }
    }
}

private fun formatRankDuration(rank: EliteRank): String {
    val minMinutes = rank.minMinutes
    return when {
        minMinutes == 0L -> "시작"
        minMinutes < 60 -> "${minMinutes}분+"
        minMinutes < 1440 -> "${minMinutes / 60}시간+"
        else -> "${minMinutes / 1440}일+"
    }
}

