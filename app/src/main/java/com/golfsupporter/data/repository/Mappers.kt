package com.golfsupporter.data.repository

import com.golfsupporter.data.local.entity.GameSessionEntity
import com.golfsupporter.data.local.entity.GameSettingsEntity
import com.golfsupporter.data.local.entity.HoleConfigEntity
import com.golfsupporter.data.local.entity.HoleScoreEntity
import com.golfsupporter.data.local.entity.PenaltyRecordEntity
import com.golfsupporter.data.local.entity.PenaltyTypeEntity
import com.golfsupporter.data.local.entity.PlayerEntity
import com.golfsupporter.data.local.entity.ScoreEditEntity
import com.golfsupporter.data.model.EditType
import com.golfsupporter.data.model.GameSettings
import com.golfsupporter.data.model.GameState
import com.golfsupporter.data.model.HoleConfig
import com.golfsupporter.data.model.HoleScore
import com.golfsupporter.data.model.PenaltyRecord
import com.golfsupporter.data.model.PenaltyType
import com.golfsupporter.data.model.Player
import com.golfsupporter.data.model.RoundPhase
import com.golfsupporter.data.model.RoundType
import com.golfsupporter.data.model.ScoreEdit
import com.golfsupporter.data.model.ScoreInputMode

// ── Player ──────────────────────────────────────────────────
fun PlayerEntity.toModel() = Player(id = playerId, name = name)
fun Player.toEntity(sessionId: String) = PlayerEntity(sessionId, id, name)

// ── Hole config ─────────────────────────────────────────────
fun HoleConfigEntity.toModel() = HoleConfig(holeNumber, par)
fun HoleConfig.toEntity(sessionId: String) = HoleConfigEntity(sessionId, holeNumber, par)

// ── Hole score ──────────────────────────────────────────────
fun HoleScoreEntity.toModel() = HoleScore(holeNumber, playerScores, isConfirmed)
fun HoleScore.toEntity(sessionId: String) =
    HoleScoreEntity(sessionId, holeNumber, playerScores, isConfirmed)

// ── Penalty record ──────────────────────────────────────────
fun PenaltyRecordEntity.toModel() = PenaltyRecord(holeNumber, playerId, penaltyTypeId, count)
fun PenaltyRecord.toEntity(sessionId: String) =
    PenaltyRecordEntity(sessionId, holeNumber, playerId, penaltyTypeId, count)

// ── Penalty type ────────────────────────────────────────────
fun PenaltyTypeEntity.toModel() = PenaltyType(
    id, label, emoji, description, isCustom, isEnabledByDefault, sortOrder, isScoreLinked
)
fun PenaltyType.toEntity() = PenaltyTypeEntity(
    id, label, emoji, description, isCustom, isEnabledByDefault, sortOrder, isScoreLinked
)

// ── Settings ────────────────────────────────────────────────
fun GameSettingsEntity.toModel() = GameSettings(
    roundType = RoundType.valueOf(roundType),
    scoreInputMode = ScoreInputMode.valueOf(scoreInputMode),
    penaltyEnabled = penaltyEnabled,
    activePenaltyIds = activePenaltyIds
)
fun GameSettings.toEntity(sessionId: String) = GameSettingsEntity(
    sessionId = sessionId,
    roundType = roundType.name,
    scoreInputMode = scoreInputMode.name,
    penaltyEnabled = penaltyEnabled,
    activePenaltyIds = activePenaltyIds
)

// ── Score edit ──────────────────────────────────────────────
fun ScoreEditEntity.toModel() = ScoreEdit(
    id, sessionId, EditType.valueOf(editType), holeNumber, playerId,
    penaltyTypeId, originalValue, newValue, editedAt
)
fun ScoreEdit.toEntity() = ScoreEditEntity(
    id, sessionId, editType.name, holeNumber, playerId,
    penaltyTypeId, originalValue, newValue, editedAt
)

// ── Game state (carried on the session entity) ──────────────
fun GameSessionEntity.toState() = GameState(
    currentHole = currentHole,
    roundPhase = RoundPhase.valueOf(roundPhase),
    isCompleted = isCompleted,
    frontNineCompletedAt = frontNineCompletedAt,
    partialScores = partialScores
)
