package com.elites.fullcharge.data

import com.elites.fullcharge.R

/**
 * 계급별 사용 가능한 이모지 정의
 *
 * - 부사관(하사~원사): nco_01 ~ nco_13, nco_08_1, nco_08_2 (15개)
 * - 위관급(소위~대위): co_01 ~ co_03 (3개)
 * - 영관급(소령~대령): 추후 추가
 * - 장성급(준장~대장): 휘장 시스템 (추후 구현)
 */
object RankEmoji {

    /**
     * 이모지 아이템
     */
    data class EmojiItem(
        val id: Int,
        val drawableResId: Int,
        val displayName: String
    )

    // 부사관용 이모지 (하사~원사) - 15개
    private val NCO_EMOJIS = listOf(
        EmojiItem(1, R.drawable.nco_01, "악!"),
        EmojiItem(2, R.drawable.nco_02, "깡!"),
        EmojiItem(3, R.drawable.nco_03, "버!"),
        EmojiItem(4, R.drawable.nco_04, "좋!"),
        EmojiItem(5, R.drawable.nco_05, "아!"),
        EmojiItem(6, R.drawable.nco_06, "요!"),
        EmojiItem(7, R.drawable.nco_07, "부사관 7"),
        EmojiItem(8, R.drawable.nco_08, "부사관 8"),
        EmojiItem(9, R.drawable.nco_08_1, "부사관 8-1"),
        EmojiItem(10, R.drawable.nco_08_2, "부사관 8-2"),
        EmojiItem(11, R.drawable.nco_09, "부사관 9"),
        EmojiItem(12, R.drawable.nco_10, "부사관 10"),
        EmojiItem(13, R.drawable.nco_11, "같!"),
        EmojiItem(14, R.drawable.nco_12, "다!"),
        EmojiItem(15, R.drawable.nco_13, "마!")
    )

    // 장교용 이모지 (소위 이상) - 3개
    private val COMPANY_OFFICER_EMOJIS = listOf(
        EmojiItem(101, R.drawable.co_01, "장교 1"),
        EmojiItem(102, R.drawable.co_02, "장교 2"),
        EmojiItem(103, R.drawable.co_03, "장교 3")
    )

    // 영관급용 이모지 (소령~대령) - 추후 추가
    private val FIELD_OFFICER_EMOJIS = listOf<EmojiItem>()

    /**
     * 현재 계급에서 사용 가능한 이모지 목록 반환
     */
    fun getAvailableEmojis(rank: EliteRank): List<EmojiItem> {
        return when (rank) {
            // 병사급: 이모지 사용 불가
            EliteRank.TRAINEE,
            EliteRank.PRIVATE_SECOND,
            EliteRank.PRIVATE_FIRST,
            EliteRank.CORPORAL,
            EliteRank.SERGEANT -> emptyList()

            // 부사관급: NCO 이모지
            EliteRank.STAFF_SERGEANT,
            EliteRank.SERGEANT_FIRST,
            EliteRank.MASTER_SERGEANT,
            EliteRank.SERGEANT_MAJOR -> NCO_EMOJIS

            // 위관급: NCO + 장교 이모지
            EliteRank.SECOND_LIEUTENANT,
            EliteRank.FIRST_LIEUTENANT,
            EliteRank.CAPTAIN -> NCO_EMOJIS + COMPANY_OFFICER_EMOJIS

            // 영관급 이상: 전체 이모지
            EliteRank.MAJOR,
            EliteRank.LIEUTENANT_COLONEL,
            EliteRank.COLONEL,
            EliteRank.BRIGADIER_GENERAL,
            EliteRank.MAJOR_GENERAL,
            EliteRank.LIEUTENANT_GENERAL,
            EliteRank.GENERAL -> NCO_EMOJIS + COMPANY_OFFICER_EMOJIS + FIELD_OFFICER_EMOJIS
        }
    }

    /**
     * 이모지 사용 가능 여부 (하사 이상만 사용 가능)
     */
    fun canUseEmoji(rank: EliteRank): Boolean {
        return rank.ordinal >= EliteRank.STAFF_SERGEANT.ordinal
    }

    /**
     * 특정 이모지 ID 사용 가능 여부
     */
    fun canUseEmojiId(rank: EliteRank, emojiId: Int): Boolean {
        val availableEmojis = getAvailableEmojis(rank)
        return availableEmojis.any { it.id == emojiId }
    }

    /**
     * 이모지 ID로 drawable 리소스 ID 가져오기
     */
    fun getEmojiDrawableResId(emojiId: Int): Int? {
        val allEmojis = NCO_EMOJIS + COMPANY_OFFICER_EMOJIS + FIELD_OFFICER_EMOJIS
        return allEmojis.find { it.id == emojiId }?.drawableResId
    }

    /**
     * 모든 이모지 목록
     */
    fun getAllEmojis(): List<EmojiItem> {
        return NCO_EMOJIS + COMPANY_OFFICER_EMOJIS + FIELD_OFFICER_EMOJIS
    }

    /**
     * 이모지 사용을 위한 최소 계급 반환
     */
    fun getMinimumRankForEmoji(emojiId: Int): EliteRank {
        return when (emojiId) {
            in 1..15 -> EliteRank.STAFF_SERGEANT  // 부사관용
            in 101..103 -> EliteRank.SECOND_LIEUTENANT  // 위관급용
            in 201..300 -> EliteRank.MAJOR  // 영관급용 (추후)
            else -> EliteRank.GENERAL
        }
    }

    /**
     * 이모지 잠금 해제까지 필요한 계급 설명
     */
    fun getUnlockRequirement(emojiId: Int): String {
        val minRank = getMinimumRankForEmoji(emojiId)
        return "${minRank.koreanName} 이상 사용 가능"
    }

    /**
     * 메시지에서 사용 불가능한 이모지 태그 제거
     * 계급에 맞지 않는 [emoji:X] 태그를 빈 문자열로 치환
     */
    fun filterUnauthorizedEmojis(message: String, rank: EliteRank): String {
        val emojiPattern = "\\[emoji:(\\d+)]".toRegex()
        return emojiPattern.replace(message) { match ->
            val emojiId = match.groupValues[1].toIntOrNull()
            if (emojiId != null && canUseEmojiId(rank, emojiId)) {
                match.value  // 사용 가능하면 그대로 유지
            } else {
                ""  // 사용 불가하면 제거
            }
        }
    }
}
