package com.golfsupporter.util

import com.golfsupporter.data.model.RoundType

/**
 * Centralises the hole-range and phase logic for each round type so the
 * ViewModels and UI stay in agreement (PRD Sections 3.1 / 3.4).
 */
object RoundRules {

    fun holeRange(type: RoundType): IntRange = when (type) {
        RoundType.FULL_18 -> 1..18
        RoundType.FRONT_9 -> 1..9
        RoundType.BACK_9 -> 10..18
        RoundType.SPLIT -> 1..18
    }

    fun firstHole(type: RoundType): Int = holeRange(type).first
    fun lastHole(type: RoundType): Int = holeRange(type).last

    /** Round types that pause for the front-nine interstitial after hole 9. */
    fun hasInterstitial(type: RoundType): Boolean =
        type == RoundType.FULL_18 || type == RoundType.SPLIT

    fun isFrontNine(hole: Int): Boolean = hole in 1..9
    fun isBackNine(hole: Int): Boolean = hole in 10..18

    /** Clamp a par-relative score to a sensible range for the given par. */
    fun clampRelative(par: Int, relative: Int): Int {
        val min = 1 - par      // best possible is 1 stroke (hole-in-one)
        val max = 10           // generous upper bound
        return relative.coerceIn(min, max)
    }
}
