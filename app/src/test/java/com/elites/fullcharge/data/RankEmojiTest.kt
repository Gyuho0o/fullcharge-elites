package com.elites.fullcharge.data

import org.junit.Assert.*
import org.junit.Test

/**
 * RankEmoji 계급별 이모지 제한 테스트
 */
class RankEmojiTest {

    // ========== canUseEmoji 테스트 ==========

    @Test
    fun `병사급은 이모지 사용 불가`() {
        assertFalse(RankEmoji.canUseEmoji(EliteRank.TRAINEE))
        assertFalse(RankEmoji.canUseEmoji(EliteRank.PRIVATE_SECOND))
        assertFalse(RankEmoji.canUseEmoji(EliteRank.PRIVATE_FIRST))
        assertFalse(RankEmoji.canUseEmoji(EliteRank.CORPORAL))
        assertFalse(RankEmoji.canUseEmoji(EliteRank.SERGEANT))
    }

    @Test
    fun `부사관급은 이모지 사용 가능`() {
        assertTrue(RankEmoji.canUseEmoji(EliteRank.STAFF_SERGEANT))
        assertTrue(RankEmoji.canUseEmoji(EliteRank.SERGEANT_FIRST))
        assertTrue(RankEmoji.canUseEmoji(EliteRank.MASTER_SERGEANT))
        assertTrue(RankEmoji.canUseEmoji(EliteRank.SERGEANT_MAJOR))
    }

    @Test
    fun `장교급은 이모지 사용 가능`() {
        assertTrue(RankEmoji.canUseEmoji(EliteRank.SECOND_LIEUTENANT))
        assertTrue(RankEmoji.canUseEmoji(EliteRank.CAPTAIN))
        assertTrue(RankEmoji.canUseEmoji(EliteRank.MAJOR))
        assertTrue(RankEmoji.canUseEmoji(EliteRank.GENERAL))
    }

    // ========== getAvailableEmojis 테스트 ==========

    @Test
    fun `병사급은 이모지 목록이 비어있음`() {
        assertTrue(RankEmoji.getAvailableEmojis(EliteRank.TRAINEE).isEmpty())
        assertTrue(RankEmoji.getAvailableEmojis(EliteRank.SERGEANT).isEmpty())
    }

    @Test
    fun `부사관급은 NCO 이모지만 사용 가능`() {
        val emojis = RankEmoji.getAvailableEmojis(EliteRank.STAFF_SERGEANT)
        assertEquals(12, emojis.size)
        assertTrue(emojis.all { it.id in 1..12 })
    }

    @Test
    fun `위관급은 NCO와 장교 이모지 사용 가능`() {
        val emojis = RankEmoji.getAvailableEmojis(EliteRank.SECOND_LIEUTENANT)
        assertEquals(15, emojis.size)  // 12 NCO + 3 장교
        assertTrue(emojis.any { it.id in 1..12 })  // NCO
        assertTrue(emojis.any { it.id in 101..103 })  // 장교
    }

    @Test
    fun `영관급 이상은 전체 이모지 사용 가능`() {
        val emojis = RankEmoji.getAvailableEmojis(EliteRank.MAJOR)
        assertTrue(emojis.size >= 15)  // 최소 NCO + 장교
    }

    // ========== canUseEmojiId 테스트 ==========

    @Test
    fun `병사급은 어떤 이모지 ID도 사용 불가`() {
        assertFalse(RankEmoji.canUseEmojiId(EliteRank.TRAINEE, 1))
        assertFalse(RankEmoji.canUseEmojiId(EliteRank.TRAINEE, 101))
    }

    @Test
    fun `부사관급은 NCO 이모지 ID만 사용 가능`() {
        assertTrue(RankEmoji.canUseEmojiId(EliteRank.STAFF_SERGEANT, 1))
        assertTrue(RankEmoji.canUseEmojiId(EliteRank.STAFF_SERGEANT, 12))
        assertFalse(RankEmoji.canUseEmojiId(EliteRank.STAFF_SERGEANT, 101))
    }

    @Test
    fun `위관급은 장교 이모지 ID도 사용 가능`() {
        assertTrue(RankEmoji.canUseEmojiId(EliteRank.CAPTAIN, 1))
        assertTrue(RankEmoji.canUseEmojiId(EliteRank.CAPTAIN, 101))
        assertTrue(RankEmoji.canUseEmojiId(EliteRank.CAPTAIN, 103))
    }

    // ========== filterUnauthorizedEmojis 테스트 ==========

    @Test
    fun `병사급 사용자의 이모지 태그가 제거됨`() {
        val message = "안녕하세요 [emoji:1] 반갑습니다 [emoji:101]"
        val filtered = RankEmoji.filterUnauthorizedEmojis(message, EliteRank.TRAINEE)
        assertEquals("안녕하세요  반갑습니다 ", filtered)
    }

    @Test
    fun `부사관급은 NCO 이모지 유지 장교 이모지 제거`() {
        val message = "테스트 [emoji:1] 메시지 [emoji:101]"
        val filtered = RankEmoji.filterUnauthorizedEmojis(message, EliteRank.STAFF_SERGEANT)
        assertEquals("테스트 [emoji:1] 메시지 ", filtered)
    }

    @Test
    fun `위관급은 모든 이모지 유지`() {
        val message = "테스트 [emoji:1] 메시지 [emoji:101]"
        val filtered = RankEmoji.filterUnauthorizedEmojis(message, EliteRank.CAPTAIN)
        assertEquals(message, filtered)
    }

    @Test
    fun `이모지 태그가 없는 메시지는 그대로 유지`() {
        val message = "일반 메시지입니다"
        val filtered = RankEmoji.filterUnauthorizedEmojis(message, EliteRank.TRAINEE)
        assertEquals(message, filtered)
    }

    // ========== getMinimumRankForEmoji 테스트 ==========

    @Test
    fun `NCO 이모지는 하사 이상 필요`() {
        assertEquals(EliteRank.STAFF_SERGEANT, RankEmoji.getMinimumRankForEmoji(1))
        assertEquals(EliteRank.STAFF_SERGEANT, RankEmoji.getMinimumRankForEmoji(12))
    }

    @Test
    fun `장교 이모지는 소위 이상 필요`() {
        assertEquals(EliteRank.SECOND_LIEUTENANT, RankEmoji.getMinimumRankForEmoji(101))
        assertEquals(EliteRank.SECOND_LIEUTENANT, RankEmoji.getMinimumRankForEmoji(103))
    }

    @Test
    fun `영관급 이모지는 소령 이상 필요`() {
        assertEquals(EliteRank.MAJOR, RankEmoji.getMinimumRankForEmoji(201))
    }
}
