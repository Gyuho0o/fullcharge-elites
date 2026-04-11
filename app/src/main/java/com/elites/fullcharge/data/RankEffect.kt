package com.elites.fullcharge.data

/**
 * 계급별 이펙트 시스템 헬퍼
 *
 * 현재는 타이핑 스파크만 모든 계급에 상시 활성화
 * 새로운 이펙트 시스템은 추후 구현 예정
 */
object RankEffect {

    // ========== 계급 판별 헬퍼 ==========

    /** 부사관 계급 여부 (하사~원사, ordinal 5-8) */
    fun isNcoRank(rank: EliteRank): Boolean {
        return rank.ordinal in 5..8
    }

    /** 장교 계급 여부 (소위~대장, ordinal 9+) */
    fun isOfficerRank(rank: EliteRank): Boolean {
        return rank.ordinal >= 9
    }

    /** 부사관 이상 계급 여부 (하사 이상) */
    fun canUseNcoEffects(rank: EliteRank): Boolean {
        return rank.ordinal >= EliteRank.STAFF_SERGEANT.ordinal
    }

    /** 장교 계급 여부 (소위 이상) */
    fun canUseOfficerEffects(rank: EliteRank): Boolean {
        return rank.ordinal >= EliteRank.SECOND_LIEUTENANT.ordinal
    }
}
