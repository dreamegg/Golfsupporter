package com.golfsupporter.data.model

/**
 * Domain models for the Golf Score Tracker. These mirror the data model defined
 * in the PRD (Section 5) and are decoupled from the Room persistence entities.
 */

// ─── Round / input enums ─────────────────────────────────────────────
enum class RoundType { FULL_18, FRONT_9, BACK_9, SPLIT }

enum class ScoreInputMode { BUTTON, SCROLL }

enum class RoundPhase {
    FRONT_NINE,        // 전반 진행 중 (홀 1~9)
    FRONT_COMPLETED,   // 전반 완료, 후반 미시작 (SPLIT 대기)
    BACK_NINE,         // 후반 진행 중 (홀 10~18)
    COMPLETED          // 전체 완료
}

enum class EditType { SCORE, PENALTY }

// ─── Player ──────────────────────────────────────────────────────────
data class Player(
    val id: Int,
    val name: String          // 최대 8자
)

// ─── Hole configuration ──────────────────────────────────────────────
data class HoleConfig(
    val holeNumber: Int,      // 1~18
    val par: Int              // 3, 4, 5
)

// ─── Hole score ──────────────────────────────────────────────────────
data class HoleScore(
    val holeNumber: Int,
    val playerScores: Map<Int, Int>,   // playerId → 파 기준 상대값
    val isConfirmed: Boolean = true    // false = 현재 홀 임시 저장
)

// ─── Penalty record ──────────────────────────────────────────────────
data class PenaltyRecord(
    val holeNumber: Int,
    val playerId: Int,
    val penaltyTypeId: String,
    val count: Int
)

// ─── Penalty type definition ─────────────────────────────────────────
data class PenaltyType(
    val id: String,
    val label: String,
    val emoji: String,
    val description: String? = null,
    val isCustom: Boolean,
    val isEnabledByDefault: Boolean,
    val sortOrder: Int,
    val isScoreLinked: Boolean = false  // 항상 false, 예비 필드
)

// ─── Game settings ───────────────────────────────────────────────────
data class GameSettings(
    val roundType: RoundType,
    val scoreInputMode: ScoreInputMode,
    val penaltyEnabled: Boolean,
    val activePenaltyIds: List<String>,
    // v2.0 — selected course location, used for the in-round weather display.
    val courseName: String? = null,
    val courseLatitude: Double? = null,
    val courseLongitude: Double? = null
)

// ─── Game state ──────────────────────────────────────────────────────
data class GameState(
    val currentHole: Int,               // 현재 진행 중인 홀 번호
    val roundPhase: RoundPhase,         // 전반/후반/완료 상태
    val isCompleted: Boolean,           // 게임 전체 완료 여부
    val frontNineCompletedAt: Long?,    // 전반 완료 시각 (SPLIT 타입)
    val partialScores: Map<Int, Int>?   // 현재 홀 임시 스코어 (onStop 저장용)
)

// ─── Score edit history ──────────────────────────────────────────────
data class ScoreEdit(
    val id: String,                    // UUID
    val sessionId: String,             // GameSession.id
    val editType: EditType,            // SCORE or PENALTY
    val holeNumber: Int,
    val playerId: Int,
    val penaltyTypeId: String? = null, // PENALTY 수정 시
    val originalValue: Int,
    val newValue: Int,
    val editedAt: Long
)

// ─── Game session (aggregate) ────────────────────────────────────────
data class GameSession(
    val id: String,                     // UUID
    val createdAt: Long,                // epoch timestamp
    val lastSavedAt: Long,              // 마지막 저장 시각 (이어하기 표시용)
    val players: List<Player>,
    val holes: List<HoleConfig>,
    val scores: List<HoleScore>,
    val penalties: List<PenaltyRecord>,
    val settings: GameSettings,
    val state: GameState
)
