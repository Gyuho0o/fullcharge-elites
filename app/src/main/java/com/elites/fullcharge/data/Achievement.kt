package com.elites.fullcharge.data

/**
 * 업적 정의
 */
enum class Achievement(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String
) {
    // 메시지 관련
    FIRST_MESSAGE(
        id = "first_message",
        emoji = "💬",
        title = "첫 마디",
        description = "첫 메시지를 보냈어요"
    ),
    CHAT_10(
        id = "chat_10",
        emoji = "🗣️",
        title = "수다쟁이",
        description = "메시지 10개 전송"
    ),
    CHAT_50(
        id = "chat_50",
        emoji = "📢",
        title = "토크 마스터",
        description = "메시지 50개 전송"
    ),
    CHAT_100(
        id = "chat_100",
        emoji = "🎙️",
        title = "방송인",
        description = "메시지 100개 전송"
    ),

    // 생존 시간 관련
    SURVIVOR_30M(
        id = "survivor_30m",
        emoji = "⏱️",
        title = "버티기",
        description = "30분 생존"
    ),
    SURVIVOR_1H(
        id = "survivor_1h",
        emoji = "🕐",
        title = "1시간의 사나이",
        description = "1시간 생존"
    ),
    SURVIVOR_3H(
        id = "survivor_3h",
        emoji = "🏃",
        title = "마라토너",
        description = "3시간 생존"
    ),
    SURVIVOR_6H(
        id = "survivor_6h",
        emoji = "🦾",
        title = "철인",
        description = "6시간 생존"
    ),

    // 위기 탈출 관련
    CRISIS_ESCAPE_1(
        id = "crisis_escape_1",
        emoji = "😅",
        title = "휴... 살았다",
        description = "첫 위기 탈출"
    ),
    CRISIS_ESCAPE_5(
        id = "crisis_escape_5",
        emoji = "😎",
        title = "위기 탈출 전문가",
        description = "위기 탈출 5회"
    ),
    CRISIS_ESCAPE_10(
        id = "crisis_escape_10",
        emoji = "🐱",
        title = "고양이 목숨",
        description = "위기 탈출 10회"
    ),

    // 계급 관련
    RANK_NCO(
        id = "rank_nco",
        emoji = "🎖️",
        title = "직업군인",
        description = "부사관(하사) 달성"
    ),
    RANK_OFFICER(
        id = "rank_officer",
        emoji = "⭐",
        title = "장교 임관",
        description = "장교(소위) 달성"
    ),
    RANK_GENERAL(
        id = "rank_general",
        emoji = "🌟",
        title = "별을 달다",
        description = "장성(준장) 달성"
    );

    companion object {
        fun fromId(id: String): Achievement? = entries.find { it.id == id }
    }
}

/**
 * 사용자 통계 (업적 달성 조건 체크용)
 */
data class UserStats(
    val totalMessages: Int = 0,
    val totalSessionTimeMs: Long = 0L,
    val crisisEscapeCount: Int = 0,
    val highestRank: String = EliteRank.TRAINEE.name,
    val unlockedAchievements: Set<String> = emptySet()
) {
    fun hasAchievement(achievement: Achievement): Boolean {
        return unlockedAchievements.contains(achievement.id)
    }
}
