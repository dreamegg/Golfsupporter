package com.golfsupporter

import com.golfsupporter.util.ScoreLabel
import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreLabelTest {

    @Test
    fun names_match_prd_table() {
        assertEquals("Albatross", ScoreLabel.nameFor(-3))
        assertEquals("Albatross", ScoreLabel.nameFor(-4))
        assertEquals("Eagle", ScoreLabel.nameFor(-2))
        assertEquals("Birdie", ScoreLabel.nameFor(-1))
        assertEquals("Par", ScoreLabel.nameFor(0))
        assertEquals("Bogey", ScoreLabel.nameFor(1))
        assertEquals("Double Bogey", ScoreLabel.nameFor(2))
        assertEquals("Triple Bogey", ScoreLabel.nameFor(3))
        assertEquals("+4", ScoreLabel.nameFor(4))
    }

    @Test
    fun short_labels() {
        assertEquals("E", ScoreLabel.shortLabel(-2))
        assertEquals("P", ScoreLabel.shortLabel(0))
        assertEquals("-1", ScoreLabel.shortLabel(-1))
        assertEquals("+2", ScoreLabel.shortLabel(2))
    }

    @Test
    fun total_formatting() {
        assertEquals("E", ScoreLabel.formatTotal(0))
        assertEquals("+3", ScoreLabel.formatTotal(3))
        assertEquals("-4", ScoreLabel.formatTotal(-4))
    }
}
