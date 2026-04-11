package com.elites.fullcharge.data

import com.elites.fullcharge.R

/**
 * 계급별 이모지 시스템
 *
 * - 공통 이모지 (8개): 모든 계급 사용 가능
 * - 부사관 이모지 (8개): 하사~원사 전용 (녹색 번개 테마)
 * - 장교 이모지 (8개): 소위 이상 전용 (보라/금색 왕관 테마)
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

    // 공통 이모지 - 모든 계급 사용 가능 (8개)
    private val COMMON_EMOJIS = listOf(
        EmojiItem(1, R.drawable.common_01, "완충!"),
        EmojiItem(2, R.drawable.common_02, "감사!"),
        EmojiItem(3, R.drawable.common_03, "최고야!"),
        EmojiItem(4, R.drawable.common_04, "전우!"),
        EmojiItem(5, R.drawable.common_05, "ㅋㅋㅋ"),
        EmojiItem(6, R.drawable.common_06, "힘..."),
        EmojiItem(7, R.drawable.common_07, "수고!"),
        EmojiItem(8, R.drawable.common_08, "완충은 사랑")
    )

    // 부사관 이모지 - 하사~원사 전용 (8개, 녹색 번개)
    private val NCO_EMOJIS = listOf(
        EmojiItem(101, R.drawable.nco_01, "충전 완료!"),
        EmojiItem(102, R.drawable.nco_02, "좋았어"),
        EmojiItem(103, R.drawable.nco_03, "간다!"),
        EmojiItem(104, R.drawable.nco_04, "지켜보지"),
        EmojiItem(105, R.drawable.nco_05, "입장"),
        EmojiItem(106, R.drawable.nco_06, "집중해라"),
        EmojiItem(107, R.drawable.nco_07, "하하하!"),
        EmojiItem(108, R.drawable.nco_08, "인정한다")
    )

    // 장교 이모지 - 소위 이상 전용 (8개, 보라/금색)
    private val OFFICER_EMOJIS = listOf(
        EmojiItem(201, R.drawable.officer_01, "충전 완료!"),
        EmojiItem(202, R.drawable.officer_02, "좋았어"),
        EmojiItem(203, R.drawable.officer_03, "간다!"),
        EmojiItem(204, R.drawable.officer_04, "지켜보지"),
        EmojiItem(205, R.drawable.officer_05, "입장"),
        EmojiItem(206, R.drawable.officer_06, "집중해라"),
        EmojiItem(207, R.drawable.officer_07, "하하하!"),
        EmojiItem(208, R.drawable.officer_08, "인정한다")
    )

    /**
     * 현재 계급에서 사용 가능한 이모지 목록 반환
     */
    fun getAvailableEmojis(rank: EliteRank): List<EmojiItem> {
        return when {
            // 장교 (소위 이상): 공통 + 부사관 + 장교
            rank.ordinal >= EliteRank.SECOND_LIEUTENANT.ordinal ->
                COMMON_EMOJIS + NCO_EMOJIS + OFFICER_EMOJIS

            // 부사관 (하사~원사): 공통 + 부사관
            rank.ordinal >= EliteRank.STAFF_SERGEANT.ordinal ->
                COMMON_EMOJIS + NCO_EMOJIS

            // 병사급: 공통만
            else -> COMMON_EMOJIS
        }
    }

    /**
     * 이모지 사용 가능 여부 (모든 계급 사용 가능)
     */
    fun canUseEmoji(rank: EliteRank): Boolean {
        return true
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
        val allEmojis = COMMON_EMOJIS + NCO_EMOJIS + OFFICER_EMOJIS
        return allEmojis.find { it.id == emojiId }?.drawableResId
    }

    /**
     * 모든 이모지 목록
     */
    fun getAllEmojis(): List<EmojiItem> {
        return COMMON_EMOJIS + NCO_EMOJIS + OFFICER_EMOJIS
    }

    /**
     * 이모지 사용을 위한 최소 계급 반환
     */
    fun getMinimumRankForEmoji(emojiId: Int): EliteRank {
        return when (emojiId) {
            in 1..100 -> EliteRank.TRAINEE           // 공통: 모든 계급
            in 101..200 -> EliteRank.STAFF_SERGEANT  // 부사관용: 하사 이상
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
