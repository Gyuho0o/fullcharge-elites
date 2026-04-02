package com.elites.fullcharge.data

import java.util.concurrent.TimeUnit

enum class EliteRank(
    val koreanName: String,
    val description: String,
    val minMinutes: Long,
    val maxMinutes: Long
) {
    NEWBIE(
        koreanName = "뉴비",
        description = "일단 들어오셨군요",
        minMinutes = 0,
        maxMinutes = 10
    ),
    PRIVATE(
        koreanName = "정회원",
        description = "슬슬 감이 잡히시나요?",
        minMinutes = 10,
        maxMinutes = 60
    ),
    SERGEANT(
        koreanName = "터줏대감",
        description = "이제 충전기를 못 뺀다",
        minMinutes = 60,
        maxMinutes = 60 * 24
    ),
    GOD(
        koreanName = "전설",
        description = "당신이 전설입니다",
        minMinutes = 60 * 24,
        maxMinutes = Long.MAX_VALUE
    );

    companion object {
        fun fromDuration(durationMillis: Long): EliteRank {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
            return entries.find { minutes >= it.minMinutes && minutes < it.maxMinutes }
                ?: GOD
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
