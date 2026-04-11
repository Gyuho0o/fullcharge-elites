package com.elites.fullcharge.data

/**
 * 채팅방 실시간 이벤트
 */
sealed class ChatEvent {
    // 개인 이벤트 (다른 사람에게도 보임)
    data class UserRankUp(
        val nickname: String,
        val newRank: EliteRank
    ) : ChatEvent()

    data class UserCrisisEscape(
        val nickname: String
    ) : ChatEvent()

    data class UserJoined(
        val nickname: String
    ) : ChatEvent()

    // 채팅방 전체 이벤트
    data class MessageMilestone(
        val count: Int  // 100, 500, 1000 등
    ) : ChatEvent()

    data class UserCountMilestone(
        val count: Int  // 5, 10, 20 등
    ) : ChatEvent()

    data class NewSurvivalRecord(
        val nickname: String,
        val duration: Long
    ) : ChatEvent()

    // 시간 기반 이벤트
    object HourlyChime : ChatEvent()

    object MidnightSpecial : ChatEvent()

    data class PersonalHourMilestone(
        val hours: Int
    ) : ChatEvent()

    // 콤보 이벤트
    data class ComboAchieved(
        val count: Int
    ) : ChatEvent()

    // 장교 이펙트 이벤트
    data class OfficerEntered(
        val userId: String,
        val nickname: String,
        val rank: EliteRank
    ) : ChatEvent()
}

/**
 * 콤보 상태
 */
data class ComboState(
    val currentCombo: Int = 0,
    val lastMessageTime: Long = 0L,
    val showComboEffect: Boolean = false
) {
    companion object {
        const val COMBO_TIMEOUT_MS = 5000L  // 5초 내 연속 메시지
    }
}
