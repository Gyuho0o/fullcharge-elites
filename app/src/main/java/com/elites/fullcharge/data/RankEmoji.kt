package com.elites.fullcharge.data

import com.elites.fullcharge.R

/**
 * 계급별 이모지 시스템
 *
 * - 부사관 이모지 (15개): 하사 이상 사용 가능
 * - 장교 이모지 (5개): 소위 이상 사용 가능
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

    // 부사관 이모지 - 하사 이상 전용 (15개)
    private val NCO_EMOJIS = listOf(
        EmojiItem(101, R.drawable.nco_01, "부사관 1"),
        EmojiItem(102, R.drawable.nco_02, "부사관 2"),
        EmojiItem(103, R.drawable.nco_03, "부사관 3"),
        EmojiItem(104, R.drawable.nco_04, "부사관 4"),
        EmojiItem(105, R.drawable.nco_05, "부사관 5"),
        EmojiItem(106, R.drawable.nco_06, "부사관 6"),
        EmojiItem(107, R.drawable.nco_07, "부사관 7"),
        EmojiItem(108, R.drawable.nco_08, "부사관 8"),
        EmojiItem(109, R.drawable.nco_09, "부사관 9"),
        EmojiItem(110, R.drawable.nco_10, "부사관 10"),
        EmojiItem(111, R.drawable.nco_11, "부사관 11"),
        EmojiItem(112, R.drawable.nco_12, "부사관 12"),
        EmojiItem(113, R.drawable.nco_13, "부사관 13"),
        EmojiItem(114, R.drawable.nco_14, "부사관 14"),
        EmojiItem(115, R.drawable.nco_15, "부사관 15")
    )

    // 장교 이모지 - 소위 이상 전용 (5개)
    private val OFFICER_EMOJIS = listOf(
        EmojiItem(201, R.drawable.officer_01, "장교 1"),
        EmojiItem(202, R.drawable.officer_02, "장교 2"),
        EmojiItem(203, R.drawable.officer_03, "장교 3"),
        EmojiItem(204, R.drawable.officer_04, "장교 4"),
        EmojiItem(205, R.drawable.officer_05, "장교 5")
    )

    /**
     * 현재 계급에서 사용 가능한 이모지 목록 반환
     */
    fun getAvailableEmojis(rank: EliteRank): List<EmojiItem> {
        return when {
            // 장교 (소위 이상): 부사관 + 장교
            rank.ordinal >= EliteRank.SECOND_LIEUTENANT.ordinal ->
                NCO_EMOJIS + OFFICER_EMOJIS

            // 부사관 (하사~원사): 부사관만
            rank.ordinal >= EliteRank.STAFF_SERGEANT.ordinal ->
                NCO_EMOJIS

            // 병사급: 이모지 사용 불가
            else -> emptyList()
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
        val allEmojis = NCO_EMOJIS + OFFICER_EMOJIS
        return allEmojis.find { it.id == emojiId }?.drawableResId
    }

    /**
     * 모든 이모지 목록
     */
    fun getAllEmojis(): List<EmojiItem> {
        return NCO_EMOJIS + OFFICER_EMOJIS
    }

    /**
     * 이모지 사용을 위한 최소 계급 반환
     */
    fun getMinimumRankForEmoji(emojiId: Int): EliteRank {
        return when (emojiId) {
            in 101..200 -> EliteRank.STAFF_SERGEANT    // 부사관용: 하사 이상
            in 201..300 -> EliteRank.SECOND_LIEUTENANT // 장교용: 소위 이상
            else -> EliteRank.GENERAL
        }
    }

    /**
     * 메시지에서 사용 불가능한 이모지 태그 제거
     */
    fun filterUnauthorizedEmojis(message: String, rank: EliteRank): String {
        val emojiPattern = "\\[emoji:(\\d+)]".toRegex()
        return emojiPattern.replace(message) { match ->
            val emojiId = match.groupValues[1].toIntOrNull()
            if (emojiId != null && canUseEmojiId(rank, emojiId)) {
                match.value
            } else {
                ""
            }
        }
    }
}
