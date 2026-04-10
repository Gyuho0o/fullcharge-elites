package com.elites.fullcharge.data

/**
 * 계급별 사용 가능한 말풍선 이펙트 정의
 *
 * - 부사관(하사~원사): 부사관 이펙트 5개 (3분 지속)
 * - 장교(소위~대장): 부사관 + 장교 이펙트 5개 (5분 지속)
 *
 * 이펙트 활성화 시 해당 사용자의 모든 말풍선에 효과가 적용됨
 */
object RankEffect {

    /**
     * 말풍선 이펙트 타입
     */
    enum class EffectType(
        val id: String,
        val displayName: String,
        val emoji: String,
        val durationMs: Long,
        val cooldownMs: Long,
        val description: String
    ) {
        // 부사관 이펙트 (3분 지속)
        CHARGING(
            "charging", "충전 중", "🔋",
            3 * 60 * 1000L, 0L,
            "말풍선에 충전 애니메이션"
        ),
        SPARK_BORDER(
            "spark_border", "스파크", "⚡",
            3 * 60 * 1000L, 0L,
            "말풍선 테두리에 전기 스파크"
        ),
        GLOW_GREEN(
            "glow_green", "그린 글로우", "💚",
            3 * 60 * 1000L, 0L,
            "말풍선에 초록색 발광 효과"
        ),
        PULSE(
            "pulse", "맥동", "💓",
            3 * 60 * 1000L, 0L,
            "말풍선이 부드럽게 맥동"
        ),
        ELECTRIC(
            "electric", "전류", "⚡",
            3 * 60 * 1000L, 0L,
            "말풍선에 전류가 흐르는 효과"
        ),

        // 장교 이펙트 (5분 지속)
        FULL_CHARGE(
            "full_charge", "완충", "👑",
            5 * 60 * 1000L, 0L,
            "금색 왕관 + 완충 뱃지"
        ),
        GOLDEN_AURA(
            "golden_aura", "황금 오라", "✨",
            5 * 60 * 1000L, 0L,
            "말풍선에 금색 오라 효과"
        ),
        FIRE_BORDER(
            "fire_border", "파이어", "🔥",
            5 * 60 * 1000L, 0L,
            "말풍선 테두리에 불꽃"
        ),
        RAINBOW(
            "rainbow", "레인보우", "🌈",
            5 * 60 * 1000L, 0L,
            "무지개 그라데이션 테두리"
        ),
        ELITE_GLOW(
            "elite_glow", "엘리트", "🎖️",
            5 * 60 * 1000L, 0L,
            "엘리트 전용 특수 발광"
        );

        companion object {
            fun fromId(id: String): EffectType? = entries.find { it.id == id }
        }
    }

    // 부사관용 이펙트 (하사~원사) - 5개
    private val NCO_EFFECTS = listOf(
        EffectType.CHARGING,
        EffectType.SPARK_BORDER,
        EffectType.GLOW_GREEN,
        EffectType.PULSE,
        EffectType.ELECTRIC
    )

    // 장교용 이펙트 (소위~대장) - 5개
    private val OFFICER_EFFECTS = listOf(
        EffectType.FULL_CHARGE,
        EffectType.GOLDEN_AURA,
        EffectType.FIRE_BORDER,
        EffectType.RAINBOW,
        EffectType.ELITE_GLOW
    )

    /**
     * 현재 계급에서 사용 가능한 이펙트 목록 반환
     */
    fun getAvailableEffects(rank: EliteRank): List<EffectType> {
        return when (rank) {
            // 병사급: 이펙트 사용 불가
            EliteRank.TRAINEE,
            EliteRank.PRIVATE_SECOND,
            EliteRank.PRIVATE_FIRST,
            EliteRank.CORPORAL,
            EliteRank.SERGEANT -> emptyList()

            // 부사관급: 부사관 이펙트
            EliteRank.STAFF_SERGEANT,
            EliteRank.SERGEANT_FIRST,
            EliteRank.MASTER_SERGEANT,
            EliteRank.SERGEANT_MAJOR -> NCO_EFFECTS

            // 장교급: 부사관 + 장교 이펙트
            EliteRank.SECOND_LIEUTENANT,
            EliteRank.FIRST_LIEUTENANT,
            EliteRank.CAPTAIN,
            EliteRank.MAJOR,
            EliteRank.LIEUTENANT_COLONEL,
            EliteRank.COLONEL,
            EliteRank.BRIGADIER_GENERAL,
            EliteRank.MAJOR_GENERAL,
            EliteRank.LIEUTENANT_GENERAL,
            EliteRank.GENERAL -> NCO_EFFECTS + OFFICER_EFFECTS
        }
    }

    /**
     * 이펙트 사용 가능 여부 (하사 이상만 사용 가능)
     */
    fun canUseEffect(rank: EliteRank): Boolean {
        return rank.ordinal >= EliteRank.STAFF_SERGEANT.ordinal
    }

    /**
     * 특정 이펙트 사용 가능 여부
     */
    fun canUseEffectType(rank: EliteRank, effectType: EffectType): Boolean {
        return getAvailableEffects(rank).contains(effectType)
    }

    /**
     * 이펙트 사용을 위한 최소 계급 반환
     */
    fun getMinimumRankForEffect(effectType: EffectType): EliteRank {
        return when (effectType) {
            in NCO_EFFECTS -> EliteRank.STAFF_SERGEANT
            in OFFICER_EFFECTS -> EliteRank.SECOND_LIEUTENANT
            else -> EliteRank.GENERAL
        }
    }

    /**
     * 이펙트 잠금 해제까지 필요한 계급 설명
     */
    fun getUnlockRequirement(effectType: EffectType): String {
        val minRank = getMinimumRankForEffect(effectType)
        return "${minRank.koreanName} 이상 사용 가능"
    }

    /**
     * 부사관 이펙트 여부
     */
    fun isNcoEffect(effectType: EffectType): Boolean {
        return effectType in NCO_EFFECTS
    }

    /**
     * 장교 이펙트 여부
     */
    fun isOfficerEffect(effectType: EffectType): Boolean {
        return effectType in OFFICER_EFFECTS
    }
}
