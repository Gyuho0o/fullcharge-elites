package com.elites.fullcharge.data

/**
 * 계급별 자동 이펙트 시스템
 *
 * - 부사관(하사~원사): 내 화면에서 멋있다 (로컬 전용, 상시 활성화)
 * - 장교(소위~대장): 남의 화면까지 흔든다 (Firebase 동기화, 이벤트성)
 *
 * 이펙트는 계급에 따라 자동으로 활성화되며, 사용자가 선택할 필요 없음
 */
object RankEffect {

    /**
     * 부사관 이펙트 (ordinal 5-8: 하사~원사)
     * - 로컬 전용: 자신의 화면에서만 표시
     * - 상시 활성화: 항상 효과 적용
     */
    enum class NcoEffect(
        val id: String,
        val displayName: String,
        val description: String
    ) {
        /** 입력 시 커서 주변에 작은 스파크, 전송 직전 강해짐 */
        TYPING_SPARK(
            "typing_spark",
            "타이핑 스파크",
            "입력할 때 커서 주변에 작은 스파크가 튐"
        ),

        /** 내 말풍선 border에 얇은 전류가 불규칙하게 흐름 */
        LIGHTNING_BORDER(
            "lightning_border",
            "번개 테두리",
            "내 말풍선 테두리에 전류가 흐름"
        ),

        /** 메시지 보낼 때 전송 버튼에서 작은 전기 폭발 */
        SEND_BURST(
            "send_burst",
            "전송 버스트",
            "메시지 전송 시 전기 폭발 효과"
        )
    }

    /**
     * 장교 이펙트 (ordinal 9+: 소위~대장)
     * - Firebase 동기화: 다른 사용자 화면에 표시
     * - 이벤트성: 특정 상황에서만 발동
     */
    enum class OfficerEffect(
        val id: String,
        val displayName: String,
        val description: String
    ) {
        /** 장교 메시지가 도착하면 말풍선 주변으로 shockwave 확산 */
        ARRIVAL_SHOCKWAVE(
            "arrival_shockwave",
            "도착 충격파",
            "장교 메시지 도착 시 충격파가 퍼짐"
        ),

        /** 장교 메시지 주변 영역이 짧게 맥동 */
        AUTHORITY_PULSE(
            "authority_pulse",
            "권위 펄스",
            "장교 메시지 주변이 맥동함"
        ),

        /** 장교 입장 시 전체 화면에 짧은 flash */
        ENTRANCE_TAKEOVER(
            "entrance_takeover",
            "입장 장악",
            "장교 입장 시 전체 화면 flash"
        )
    }

    // ========== 계급 판별 헬퍼 ==========

    /** 부사관 계급 여부 (하사~원사, ordinal 5-8) */
    fun isNcoRank(rank: EliteRank): Boolean {
        return rank.ordinal in 5..8
    }

    /** 장교 계급 여부 (소위~대장, ordinal 9+) */
    fun isOfficerRank(rank: EliteRank): Boolean {
        return rank.ordinal >= 9
    }

    /** 부사관 이펙트 사용 가능 여부 (하사 이상) */
    fun canUseNcoEffects(rank: EliteRank): Boolean {
        return rank.ordinal >= EliteRank.STAFF_SERGEANT.ordinal
    }

    /** 장교 이펙트 사용 가능 여부 (소위 이상) */
    fun canUseOfficerEffects(rank: EliteRank): Boolean {
        return rank.ordinal >= EliteRank.SECOND_LIEUTENANT.ordinal
    }
}
