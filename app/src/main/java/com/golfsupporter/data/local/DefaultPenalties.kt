package com.golfsupporter.data.local

import com.golfsupporter.data.local.entity.PenaltyTypeEntity

/**
 * The 8 built-in penalty types seeded into the DB on first launch
 * (PRD Section 3.5). Built-in types cannot be deleted.
 */
object DefaultPenalties {

    val list: List<PenaltyTypeEntity> = listOf(
        PenaltyTypeEntity("OB", "OB", "🚫", null, isCustom = false, isEnabledByDefault = true, sortOrder = 0, isScoreLinked = false),
        PenaltyTypeEntity("BUNKER", "벙커", "🏖️", null, isCustom = false, isEnabledByDefault = true, sortOrder = 1, isScoreLinked = false),
        PenaltyTypeEntity("THREE_PUTT", "3퍼터", "🔁", null, isCustom = false, isEnabledByDefault = true, sortOrder = 2, isScoreLinked = false),
        PenaltyTypeEntity("WATER", "워터해저드", "💧", null, isCustom = false, isEnabledByDefault = true, sortOrder = 3, isScoreLinked = false),
        PenaltyTypeEntity("MULLIGAN", "멀리건", "🔄", null, isCustom = false, isEnabledByDefault = true, sortOrder = 4, isScoreLinked = false),
        PenaltyTypeEntity("FAIRWAY_MISS", "페어웨이 미스", "🌿", null, isCustom = false, isEnabledByDefault = true, sortOrder = 5, isScoreLinked = false),
        PenaltyTypeEntity("PENALTY_AREA", "페널티존", "⚠️", null, isCustom = false, isEnabledByDefault = false, sortOrder = 6, isScoreLinked = false),
        PenaltyTypeEntity("LOST_BALL", "분실구", "🔍", null, isCustom = false, isEnabledByDefault = false, sortOrder = 7, isScoreLinked = false),
    )
}
