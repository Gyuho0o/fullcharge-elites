package com.elites.fullcharge.data

/**
 * 계급별 사용 가능한 이펙트 정의
 *
 * - 부사관(하사~원사): 부사관 이펙트 4개
 * - 장교(소위~대장): 부사관 + 장교 이펙트 4개
 */
object RankEffect {

    /**
     * 이펙트 타입
     */
    enum class EffectType(
        val id: String,
        val displayName: String,
        val emoji: String,
        val durationMs: Long,
        val cooldownMs: Long
    ) {
        // 부사관 이펙트 (1.5~2초) - TODO: 테스트 후 쿨다운 복원 (30_000L, 60_000L)
        CHARGE("charge", "충전", "🔋", 2000L, 0L),
        BULB("bulb", "전구", "💡", 1800L, 0L),
        PLUG("plug", "플러그", "🔌", 1500L, 0L),
        FULL_CHARGE("full_charge", "완충 선언", "👑", 2500L, 0L),
        CURRENT("current", "전류", "⚡", 1800L, 0L),
        POWER_SURGE("power_surge", "파워 서지", "🔆", 2000L, 0L),

        // 장교 이펙트 (2.5~3.5초)
        LIGHTNING_STORM("lightning_storm", "번개 폭풍", "🌩️", 3000L, 0L),
        ENERGY_BURST("energy_burst", "에너지 버스트", "💥", 2500L, 0L),
        DOUBLE_LIGHTNING("double_lightning", "더블 라이트닝", "⚡⚡", 2500L, 0L),
        THUNDERBOLT("thunderbolt", "썬더볼트", "⛈️", 3000L, 0L),
        OVERCHARGE("overcharge", "오버차지", "🌟", 3500L, 0L);

        companion object {
            fun fromId(id: String): EffectType? = entries.find { it.id == id }
        }
    }

    // 부사관용 이펙트 (하사~원사) - 6개
    private val NCO_EFFECTS = listOf(
        EffectType.CHARGE,
        EffectType.BULB,
        EffectType.PLUG,
        EffectType.FULL_CHARGE,
        EffectType.CURRENT,
        EffectType.POWER_SURGE
    )

    // 장교용 이펙트 (소위~대장) - 5개
    private val OFFICER_EFFECTS = listOf(
        EffectType.LIGHTNING_STORM,
        EffectType.ENERGY_BURST,
        EffectType.DOUBLE_LIGHTNING,
        EffectType.THUNDERBOLT,
        EffectType.OVERCHARGE
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
