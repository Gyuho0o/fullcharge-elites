package com.elites.fullcharge.data

import org.junit.Assert.*
import org.junit.Test

/**
 * RankEmoji 이모지 시스템 테스트
 *
 * - 공통 이모지 (8개): 모든 계급 사용 가능
 * - 부사관 이모지 (8개): 하사 이상 사용 가능
 * - 장교 이모지 (8개): 소위 이상 사용 가능
 */
class RankEmojiTest {

    // ========== canUseEmoji 테스트 ==========

    @Test
    fun `모든 계급은 이모지 사용 가능`() {
        assertTrue(RankEmoji.canUseEmoji(EliteRank.TRAINEE))
        assertTrue(RankEmoji.canUseEmoji(EliteRank.SERGEANT))
        assertTrue(RankEmoji.canUseEmoji(EliteRank.STAFF_SERGEANT))
        assertTrue(RankEmoji.canUseEmoji(EliteRank.SECOND_LIEUTENANT))
    }

    // ========== getAvailableEmojis 테스트 ==========

    @Test
    fun `병사급은 공통 이모지만 사용 가능`() {
        val emojis = RankEmoji.getAvailableEmojis(EliteRank.TRAINEE)
        assertEquals(8, emojis.size)
        assertTrue(emojis.all { it.id in 1..100 })
    }

    @Test
    fun `부사관은 공통 + 부사관 이모지 사용 가능`() {
        val emojis = RankEmoji.getAvailableEmojis(EliteRank.STAFF_SERGEANT)
        assertEquals(16, emojis.size)
        assertTrue(emojis.any { it.id in 1..100 })
        assertTrue(emojis.any { it.id in 101..200 })
        assertFalse(emojis.any { it.id in 201..300 })
    }

    @Test
    fun `장교는 모든 이모지 사용 가능`() {
        val emojis = RankEmoji.getAvailableEmojis(EliteRank.CAPTAIN)
        assertEquals(24, emojis.size)
        assertTrue(emojis.any { it.id in 1..100 })
        assertTrue(emojis.any { it.id in 101..200 })
        assertTrue(emojis.any { it.id in 201..300 })
    }

    // ========== canUseEmojiId 테스트 ==========

    @Test
    fun `병사는 공통 이모지만 사용 가능`() {
        assertTrue(RankEmoji.canUseEmojiId(EliteRank.TRAINEE, 1))
        assertFalse(RankEmoji.canUseEmojiId(EliteRank.TRAINEE, 101))
        assertFalse(RankEmoji.canUseEmojiId(EliteRank.TRAINEE, 201))
    }

    @Test
    fun `부사관은 부사관 이모지까지 사용 가능`() {
        assertTrue(RankEmoji.canUseEmojiId(EliteRank.STAFF_SERGEANT, 1))
        assertTrue(RankEmoji.canUseEmojiId(EliteRank.STAFF_SERGEANT, 101))
        assertFalse(RankEmoji.canUseEmojiId(EliteRank.STAFF_SERGEANT, 201))
    }

    @Test
    fun `장교는 모든 이모지 ID 사용 가능`() {
        assertTrue(RankEmoji.canUseEmojiId(EliteRank.CAPTAIN, 1))
        assertTrue(RankEmoji.canUseEmojiId(EliteRank.CAPTAIN, 101))
        assertTrue(RankEmoji.canUseEmojiId(EliteRank.CAPTAIN, 201))
    }

    // ========== getMinimumRankForEmoji 테스트 ==========

    @Test
    fun `공통 이모지는 모든 계급 사용 가능`() {
        assertEquals(EliteRank.TRAINEE, RankEmoji.getMinimumRankForEmoji(1))
        assertEquals(EliteRank.TRAINEE, RankEmoji.getMinimumRankForEmoji(8))
    }

    @Test
    fun `부사관 이모지는 하사 이상 필요`() {
        assertEquals(EliteRank.STAFF_SERGEANT, RankEmoji.getMinimumRankForEmoji(101))
        assertEquals(EliteRank.STAFF_SERGEANT, RankEmoji.getMinimumRankForEmoji(108))
    }

    @Test
    fun `장교 이모지는 소위 이상 필요`() {
        assertEquals(EliteRank.SECOND_LIEUTENANT, RankEmoji.getMinimumRankForEmoji(201))
        assertEquals(EliteRank.SECOND_LIEUTENANT, RankEmoji.getMinimumRankForEmoji(208))
    }

    // ========== filterUnauthorizedEmojis 테스트 ==========

    @Test
    fun `병사는 부사관 이모지가 필터링됨`() {
        val message = "테스트 [emoji:1] 메시지 [emoji:101]"
        val filtered = RankEmoji.filterUnauthorizedEmojis(message, EliteRank.TRAINEE)
        assertEquals("테스트 [emoji:1] 메시지 ", filtered)
    }

    @Test
    fun `장교는 모든 이모지 허용`() {
        val message = "테스트 [emoji:1] [emoji:101] [emoji:201]"
        val filtered = RankEmoji.filterUnauthorizedEmojis(message, EliteRank.CAPTAIN)
        assertEquals(message, filtered)
    }

    // ========== getAllEmojis 테스트 ==========

    @Test
    fun `getAllEmojis는 24개의 이모지 반환`() {
        val allEmojis = RankEmoji.getAllEmojis()
        assertEquals(24, allEmojis.size)
    }

    // ========== getEmojiDrawableResId 테스트 ==========

    @Test
    fun `유효한 이모지 ID는 drawable 리소스 ID 반환`() {
        assertNotNull(RankEmoji.getEmojiDrawableResId(1))
        assertNotNull(RankEmoji.getEmojiDrawableResId(101))
        assertNotNull(RankEmoji.getEmojiDrawableResId(201))
    }

    @Test
    fun `유효하지 않은 이모지 ID는 null 반환`() {
        assertNull(RankEmoji.getEmojiDrawableResId(999))
    }
}
