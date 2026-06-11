package com.golfsupporter.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entities. Enums are stored as their name strings; collections go through
 * [com.golfsupporter.data.local.Converters].
 */

@Entity(tableName = "game_sessions")
data class GameSessionEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val lastSavedAt: Long,
    // GameState (flattened)
    val currentHole: Int,
    val roundPhase: String,
    val isCompleted: Boolean,
    val frontNineCompletedAt: Long?,
    val partialScores: Map<Int, Int>?
)

@Entity(tableName = "game_settings")
data class GameSettingsEntity(
    @PrimaryKey val sessionId: String,
    val roundType: String,
    val scoreInputMode: String,
    val penaltyEnabled: Boolean,
    val activePenaltyIds: List<String>
)

@Entity(
    tableName = "players",
    primaryKeys = ["sessionId", "playerId"]
)
data class PlayerEntity(
    val sessionId: String,
    val playerId: Int,
    val name: String
)

@Entity(
    tableName = "hole_configs",
    primaryKeys = ["sessionId", "holeNumber"]
)
data class HoleConfigEntity(
    val sessionId: String,
    val holeNumber: Int,
    val par: Int
)

@Entity(
    tableName = "hole_scores",
    primaryKeys = ["sessionId", "holeNumber"]
)
data class HoleScoreEntity(
    val sessionId: String,
    val holeNumber: Int,
    val playerScores: Map<Int, Int>,
    val isConfirmed: Boolean
)

@Entity(
    tableName = "penalty_records",
    primaryKeys = ["sessionId", "holeNumber", "playerId", "penaltyTypeId"]
)
data class PenaltyRecordEntity(
    val sessionId: String,
    val holeNumber: Int,
    val playerId: Int,
    val penaltyTypeId: String,
    val count: Int
)

@Entity(tableName = "penalty_types")
data class PenaltyTypeEntity(
    @PrimaryKey val id: String,
    val label: String,
    val emoji: String,
    val description: String?,
    val isCustom: Boolean,
    val isEnabledByDefault: Boolean,
    val sortOrder: Int,
    val isScoreLinked: Boolean
)

@Entity(
    tableName = "score_edits",
    indices = [Index("sessionId")]
)
data class ScoreEditEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val editType: String,
    val holeNumber: Int,
    val playerId: Int,
    val penaltyTypeId: String?,
    val originalValue: Int,
    val newValue: Int,
    val editedAt: Long
)
