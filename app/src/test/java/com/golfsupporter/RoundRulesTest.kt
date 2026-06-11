package com.golfsupporter

import com.golfsupporter.data.model.RoundType
import com.golfsupporter.util.RoundRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoundRulesTest {

    @Test
    fun hole_ranges_per_round_type() {
        assertEquals(1..18, RoundRules.holeRange(RoundType.FULL_18))
        assertEquals(1..9, RoundRules.holeRange(RoundType.FRONT_9))
        assertEquals(10..18, RoundRules.holeRange(RoundType.BACK_9))
        assertEquals(1..18, RoundRules.holeRange(RoundType.SPLIT))
    }

    @Test
    fun first_and_last_holes() {
        assertEquals(10, RoundRules.firstHole(RoundType.BACK_9))
        assertEquals(18, RoundRules.lastHole(RoundType.BACK_9))
        assertEquals(1, RoundRules.firstHole(RoundType.FRONT_9))
        assertEquals(9, RoundRules.lastHole(RoundType.FRONT_9))
    }

    @Test
    fun interstitial_only_for_full_and_split() {
        assertTrue(RoundRules.hasInterstitial(RoundType.FULL_18))
        assertTrue(RoundRules.hasInterstitial(RoundType.SPLIT))
        assertFalse(RoundRules.hasInterstitial(RoundType.FRONT_9))
        assertFalse(RoundRules.hasInterstitial(RoundType.BACK_9))
    }

    @Test
    fun clamp_respects_par_floor() {
        // Best possible on a par 3 is a hole-in-one: 1 - 3 = -2.
        assertEquals(-2, RoundRules.clampRelative(3, -5))
        // Best on par 5 is -4.
        assertEquals(-4, RoundRules.clampRelative(5, -10))
        assertEquals(10, RoundRules.clampRelative(4, 99))
    }
}
