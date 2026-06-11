package com.golfsupporter.util

import androidx.compose.ui.graphics.Color

/**
 * Maps a score relative to par to its golf name and a colour code, per the
 * PRD score-naming table (Section 3.2.4).
 */
object ScoreLabel {

    // Colour codes (kept theme-agnostic so they read on light & dark backgrounds).
    val Gold = Color(0xFFF9A825)
    val Blue = Color(0xFF1976D2)
    val Neutral = Color(0xFF9E9E9E)
    val Orange = Color(0xFFEF6C00)
    val Red = Color(0xFFE53935)
    val RedDark = Color(0xFFB71C1C)

    /** Relative score (par-relative). Returns the display name. */
    fun nameFor(relative: Int): String = when {
        relative <= -3 -> "Albatross"
        relative == -2 -> "Eagle"
        relative == -1 -> "Birdie"
        relative == 0 -> "Par"
        relative == 1 -> "Bogey"
        relative == 2 -> "Double Bogey"
        relative == 3 -> "Triple Bogey"
        else -> "+$relative"
    }

    fun colorFor(relative: Int): Color = when {
        relative <= -3 -> Gold
        relative == -2 -> Gold
        relative == -1 -> Blue
        relative == 0 -> Neutral
        relative == 1 -> Orange
        relative == 2 -> Red
        else -> RedDark
    }

    /** Short label for compact tables, e.g. "E", "P", "-1", "+2". */
    fun shortLabel(relative: Int): String = when (relative) {
        -2 -> "E"
        0 -> "P"
        else -> if (relative > 0) "+$relative" else relative.toString()
    }

    /** Formats a cumulative total, e.g. "-4", "E", "+2". */
    fun formatTotal(total: Int): String = when {
        total == 0 -> "E"
        total > 0 -> "+$total"
        else -> total.toString()
    }
}
