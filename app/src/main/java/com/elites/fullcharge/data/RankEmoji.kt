package com.elites.fullcharge.data

import com.elites.fullcharge.R

/**
 * 계급별 사용 가능한 이모지 정의
 *
 * - emoji_01~06: 하사~원사 (부사관 이상)
 * - emoji_07~10: 소위~대위 (위관급 이상)
 * - emoji_11~14: 소령~대령 (영관급 이상)
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

    // 부사관용 이모지 (하사~원사)
    private val NCO_EMOJIS = listOf(
        EmojiItem(1, R.drawable.emoji_01, "악!"),
        EmojiItem(6, R.drawable.emoji_06, "깡!"),
        EmojiItem(15, R.drawable.emoji_15, "버!"),
        EmojiItem(2, R.drawable.emoji_02, "좋!"),
        EmojiItem(3, R.drawable.emoji_03, "아!"),
        EmojiItem(4, R.drawable.emoji_04, "요!")
    )

    // 위관급용 이모지 (소위~대위)
    private val COMPANY_OFFICER_EMOJIS = listOf(
        EmojiItem(7, R.drawable.emoji_07, "위관 1"),
        EmojiItem(8, R.drawable.emoji_08, "위관 2"),
        EmojiItem(9, R.drawable.emoji_09, "위관 3"),
        EmojiItem(10, R.drawable.emoji_10, "위관 4")
    )

    // 영관급용 이모지 (소령~대령)
    private val FIELD_OFFICER_EMOJIS = listOf(
        EmojiItem(11, R.drawable.emoji_11, "영관 1"),
        EmojiItem(12, R.drawable.emoji_12, "영관 2"),
        EmojiItem(13, R.drawable.emoji_13, "영관 3"),
        EmojiItem(14, R.drawable.emoji_14, "영관 4")
    )

    /**
     * 현재 계급에서 사용 가능한 이모지 목록 반환
     */
    fun getAvailableEmojis(rank: EliteRank): List<EmojiItem> {
        // TODO: 테스트 완료 후 원래 로직으로 복원
        return NCO_EMOJIS + COMPANY_OFFICER_EMOJIS + FIELD_OFFICER_EMOJIS

        /*
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

            // 위관급: NCO + 위관 이모지
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
        */
    }

    /**
     * 이모지 사용 가능 여부
     */
    fun canUseEmoji(rank: EliteRank): Boolean {
        // TODO: 테스트 완료 후 원래 로직으로 복원
        return true
        // return rank.ordinal >= EliteRank.STAFF_SERGEANT.ordinal
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
            1, 2, 3, 4, 6, 15 -> EliteRank.STAFF_SERGEANT  // 부사관용
            in 7..10 -> EliteRank.SECOND_LIEUTENANT  // 위관급용
            in 11..14 -> EliteRank.MAJOR  // 영관급용
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
