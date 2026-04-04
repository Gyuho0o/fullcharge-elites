package com.elites.fullcharge.data

import java.util.concurrent.TimeUnit

enum class EliteRank(
    val koreanName: String,
    val description: String,
    val minMinutes: Long,
    val maxMinutes: Long
) {
    // 병사 (Enlisted) - 0~30분
    TRAINEE(
        koreanName = "훈련병",
        description = "훈련소 입소! 충전기 꽉 잡아!",
        minMinutes = 0,
        maxMinutes = 1
    ),
    PRIVATE_SECOND(
        koreanName = "이등병",
        description = "이등병 진급! 이제 시작이다",
        minMinutes = 1,
        maxMinutes = 3
    ),
    PRIVATE_FIRST(
        koreanName = "일등병",
        description = "일등병 진급! 아직 갈 길이 멀다",
        minMinutes = 3,
        maxMinutes = 7
    ),
    CORPORAL(
        koreanName = "상병",
        description = "상병 진급! 슬슬 감이 잡히네",
        minMinutes = 7,
        maxMinutes = 15
    ),
    SERGEANT(
        koreanName = "병장",
        description = "병장 진급! 만기 전역 각?",
        minMinutes = 15,
        maxMinutes = 30
    ),
    // 부사관 (NCO) - 30분~3시간
    STAFF_SERGEANT(
        koreanName = "하사",
        description = "하사 임관! 이제 직업군인이다",
        minMinutes = 30,
        maxMinutes = 45
    ),
    SERGEANT_FIRST(
        koreanName = "중사",
        description = "중사 진급! 부대의 기둥",
        minMinutes = 45,
        maxMinutes = 90
    ),
    MASTER_SERGEANT(
        koreanName = "상사",
        description = "상사 진급! 베테랑의 위엄",
        minMinutes = 90,
        maxMinutes = 150
    ),
    SERGEANT_MAJOR(
        koreanName = "원사",
        description = "원사 진급! 부사관의 정점",
        minMinutes = 150,
        maxMinutes = 210
    ),
    // 위관급 장교 (Company Officers) - 3.5시간~12시간
    SECOND_LIEUTENANT(
        koreanName = "소위",
        description = "소위 임관! 장교의 첫 걸음",
        minMinutes = 210,
        maxMinutes = 300
    ),
    FIRST_LIEUTENANT(
        koreanName = "중위",
        description = "중위 진급! 실전 지휘관",
        minMinutes = 300,
        maxMinutes = 480
    ),
    CAPTAIN(
        koreanName = "대위",
        description = "대위 진급! 중대장의 위엄",
        minMinutes = 480,
        maxMinutes = 720
    ),
    // 영관급 장교 (Field Officers) - 12시간~4일
    MAJOR(
        koreanName = "소령",
        description = "소령 진급! 참모의 시작",
        minMinutes = 720,
        maxMinutes = 1440
    ),
    LIEUTENANT_COLONEL(
        koreanName = "중령",
        description = "중령 진급! 대대장급 지휘관",
        minMinutes = 1440,
        maxMinutes = 2880
    ),
    COLONEL(
        koreanName = "대령",
        description = "대령 진급! 연대장의 위엄",
        minMinutes = 2880,
        maxMinutes = 5760
    ),
    // 장성 (General Officers) - 4일~7일
    BRIGADIER_GENERAL(
        koreanName = "준장",
        description = "⭐ 준장 진급! 별이 빛난다",
        minMinutes = 5760,
        maxMinutes = 7200
    ),
    MAJOR_GENERAL(
        koreanName = "소장",
        description = "⭐⭐ 소장 진급! 사단장급",
        minMinutes = 7200,
        maxMinutes = 8640
    ),
    LIEUTENANT_GENERAL(
        koreanName = "중장",
        description = "⭐⭐⭐ 중장 진급! 군단장급",
        minMinutes = 8640,
        maxMinutes = 10080
    ),
    GENERAL(
        koreanName = "대장",
        description = "⭐⭐⭐⭐ 대장! 당신이 전설입니다",
        minMinutes = 10080,
        maxMinutes = Long.MAX_VALUE
    );

    companion object {
        fun fromDuration(durationMillis: Long): EliteRank {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
            return entries.find { minutes >= it.minMinutes && minutes < it.maxMinutes }
                ?: GENERAL
        }

        fun fromDurationFormatted(durationMillis: Long): String {
            val totalSeconds = durationMillis / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60

            return when {
                hours > 0 -> String.format("%d시간 %d분 %d초", hours, minutes, seconds)
                minutes > 0 -> String.format("%d분 %d초", minutes, seconds)
                else -> String.format("%d초", seconds)
            }
        }
    }
}

data class EliteUser(
    val userId: String = "",
    val nickname: String = "",
    val sessionStartTime: Long = 0L,
    val lastActiveTime: Long = System.currentTimeMillis(),
    val isOnline: Boolean = true
) {
    val sessionDuration: Long
        get() = if (sessionStartTime > 0) {
            System.currentTimeMillis() - sessionStartTime
        } else 0L

    val rank: EliteRank
        get() = EliteRank.fromDuration(sessionDuration)

    val formattedDuration: String
        get() = EliteRank.fromDurationFormatted(sessionDuration)
}

/**
 * 역대 최장 기록
 */
data class AllTimeRecord(
    val oderId: String = "",
    val nickname: String = "",
    val durationMillis: Long = 0L,
    val achievedAt: Long = 0L
) {
    val rank: EliteRank
        get() = EliteRank.fromDuration(durationMillis)

    val formattedDuration: String
        get() = EliteRank.fromDurationFormatted(durationMillis)

    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to oderId,
        "nickname" to nickname,
        "durationMillis" to durationMillis,
        "achievedAt" to achievedAt
    )
}
