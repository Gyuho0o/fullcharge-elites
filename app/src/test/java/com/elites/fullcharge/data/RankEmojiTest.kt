package com.elites.fullcharge.data

import org.junit.Assert.*
import org.junit.Test

/**
 * RankEmoji 이모지 시스템 테스트
 *
 * - 부사관 이모지 (15개): 하사 이상 사용 가능
 * - 장교 이모지 (5개): 소위 이상 사용 가능
 */
class RankEmojiTest {

    // ========== canUseEmoji 테스트 ==========

    @Test
    fun `canUseEmoji - 병사급은 이모지 사용 불가`() {
        assertFalse(RankEmoji.canUseEmoji(EliteRank.TRAINEE))
        assertFalse(RankEmoji.canUseEmoji(EliteRank.PRIVATE_SECOND))
        assertFalse(RankEmoji.canUseEmoji(EliteRank.CORPORAL))
        assertFalse(RankEmoji.canUseEmoji(EliteRank.SERGEANT))
    }

    @Test
    fun `하사 이상은 이모지 사용 가능`() {
        assertTrue(RankEmoji.canUseEmoji(EliteRank.STAFF_SERGEANT))
        assertTrue(RankEmoji.canUseEmoji(EliteRank.SERGEANT_FIRST))
        assertTrue(RankEmoji.canUseEmoji(EliteRank.SECOND_LIEUTENANT))
        assertTrue(RankEmoji.canUseEmoji(EliteRank.CAPTAIN))
    }

    // ========== getAvailableEmojis 테스트 ==========

    @Test
    fun `getAvailableEmojis - 병사급은 빈 목록 반환`() {
        val emojis = RankEmoji.getAvailableEmojis(EliteRank.TRAINEE)
        assertEquals(0, emojis.size)
    }

    @Test
    fun `부사관은 부사관 이모지만 사용 가능`() {
        val emojis = RankEmoji.getAvailableEmojis(EliteRank.STAFF_SERGEANT)
        assertEquals(15, emojis.size)
        assertTrue(emojis.all { it.id in 101..200 })
        assertFalse(emojis.any { it.id in 201..300 })
    }

    @Test
    fun `장교는 부사관 + 장교 이모지 사용 가능`() {
        val emojis = RankEmoji.getAvailableEmojis(EliteRank.CAPTAIN)
        assertEquals(20, emojis.size)  // 15 NCO + 5 Officer
        assertTrue(emojis.any { it.id in 101..200 })
        assertTrue(emojis.any { it.id in 201..300 })
    }

    // ========== canUseEmojiId 테스트 ==========

    @Test
    fun `병사는 모든 이모지 사용 불가`() {
        assertFalse(RankEmoji.canUseEmojiId(EliteRank.TRAINEE, 101))
        assertFalse(RankEmoji.canUseEmojiId(EliteRank.TRAINEE, 201))
    }

    @Test
    fun `부사관은 부사관 이모지까지 사용 가능`() {
        assertTrue(RankEmoji.canUseEmojiId(EliteRank.STAFF_SERGEANT, 101))
        assertTrue(RankEmoji.canUseEmojiId(EliteRank.STAFF_SERGEANT, 115))
        assertFalse(RankEmoji.canUseEmojiId(EliteRank.STAFF_SERGEANT, 201))
    }

    @Test
    fun `장교는 모든 이모지 ID 사용 가능`() {
        assertTrue(RankEmoji.canUseEmojiId(EliteRank.CAPTAIN, 101))
        assertTrue(RankEmoji.canUseEmojiId(EliteRank.CAPTAIN, 115))
        assertTrue(RankEmoji.canUseEmojiId(EliteRank.CAPTAIN, 201))
        assertTrue(RankEmoji.canUseEmojiId(EliteRank.CAPTAIN, 205))
    }

    // ========== getMinimumRankForEmoji 테스트 ==========

    @Test
    fun `부사관 이모지는 하사 이상 필요`() {
        assertEquals(EliteRank.STAFF_SERGEANT, RankEmoji.getMinimumRankForEmoji(101))
        assertEquals(EliteRank.STAFF_SERGEANT, RankEmoji.getMinimumRankForEmoji(115))
    }

    @Test
    fun `장교 이모지는 소위 이상 필요`() {
        assertEquals(EliteRank.SECOND_LIEUTENANT, RankEmoji.getMinimumRankForEmoji(201))
        assertEquals(EliteRank.SECOND_LIEUTENANT, RankEmoji.getMinimumRankForEmoji(205))
    }

    // ========== filterUnauthorizedEmojis 테스트 ==========

    @Test
    fun `병사는 모든 이모지가 필터링됨`() {
        val message = "테스트 [emoji:101] 메시지 [emoji:201]"
        val filtered = RankEmoji.filterUnauthorizedEmojis(message, EliteRank.TRAINEE)
        assertEquals("테스트  메시지 ", filtered)
    }

    @Test
    fun `부사관은 장교 이모지만 필터링됨`() {
        val message = "테스트 [emoji:101] 메시지 [emoji:201]"
        val filtered = RankEmoji.filterUnauthorizedEmojis(message, EliteRank.STAFF_SERGEANT)
        assertEquals("테스트 [emoji:101] 메시지 ", filtered)
    }

    @Test
    fun `장교는 모든 이모지 허용`() {
        val message = "테스트 [emoji:101] [emoji:201]"
        val filtered = RankEmoji.filterUnauthorizedEmojis(message, EliteRank.CAPTAIN)
        assertEquals(message, filtered)
    }

    // ========== getAllEmojis 테스트 ==========

    @Test
    fun `getAllEmojis는 20개의 이모지 반환`() {
        val allEmojis = RankEmoji.getAllEmojis()
        assertEquals(20, allEmojis.size)
        // 15개 부사관 + 5개 장교
        assertEquals(15, allEmojis.count { it.id in 101..200 })
        assertEquals(5, allEmojis.count { it.id in 201..300 })
    }

    // ========== getEmojiDrawableResId 테스트 ==========

    @Test
    fun `유효한 이모지 ID는 drawable 리소스 ID 반환`() {
        assertNotNull(RankEmoji.getEmojiDrawableResId(101))
        assertNotNull(RankEmoji.getEmojiDrawableResId(115))
        assertNotNull(RankEmoji.getEmojiDrawableResId(201))
        assertNotNull(RankEmoji.getEmojiDrawableResId(205))
    }

    @Test
    fun `유효하지 않은 이모지 ID는 null 반환`() {
        assertNull(RankEmoji.getEmojiDrawableResId(1))    // 공통 이모지 없음
        assertNull(RankEmoji.getEmojiDrawableResId(999))
    }
}
