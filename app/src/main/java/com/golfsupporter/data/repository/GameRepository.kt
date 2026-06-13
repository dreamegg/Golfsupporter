package com.golfsupporter.data.repository

import com.golfsupporter.data.local.DefaultPenalties
import com.golfsupporter.data.local.dao.GameDao
import com.golfsupporter.data.local.dao.NameHistoryDao
import com.golfsupporter.data.local.dao.PenaltyTypeDao
import com.golfsupporter.data.local.entity.GameSessionEntity
import com.golfsupporter.data.local.entity.RememberedNameEntity
import com.golfsupporter.data.model.GameSession
import com.golfsupporter.data.model.GameSettings
import com.golfsupporter.data.model.GameState
import com.golfsupporter.data.model.HoleScore
import com.golfsupporter.data.model.PenaltyRecord
import com.golfsupporter.data.model.PenaltyType
import com.golfsupporter.data.model.ScoreEdit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for game data. Bridges Room and the domain models and
 * owns the incremental auto-save behaviour described in the PRD (Section 3.3).
 */
@Singleton
class GameRepository @Inject constructor(
    private val gameDao: GameDao,
    private val penaltyTypeDao: PenaltyTypeDao,
    private val nameHistoryDao: NameHistoryDao,
) {

    /** Seeds the 8 built-in penalty types on first launch. */
    suspend fun ensurePenaltiesSeeded() {
        if (penaltyTypeDao.count() == 0) {
            penaltyTypeDao.upsertAll(DefaultPenalties.list)
        }
    }

    // ── Penalty types ──────────────────────────────────────────
    fun observePenaltyTypes(): Flow<List<PenaltyType>> =
        penaltyTypeDao.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun getPenaltyTypes(): List<PenaltyType> =
        penaltyTypeDao.getAll().map { it.toModel() }

    suspend fun addCustomPenalty(type: PenaltyType) =
        penaltyTypeDao.upsert(type.toEntity())

    suspend fun deleteCustomPenalty(id: String) =
        penaltyTypeDao.deleteCustom(id)

    // ── Active / continue banner ───────────────────────────────
    fun observeActiveSession(): Flow<GameSession?> =
        gameDao.observeActiveSession().map { entity ->
            entity?.let { loadSession(it.id) }
        }

    fun observeAllSessions(): Flow<List<GameSession>> =
        gameDao.observeAllSessions().map { list -> list.mapNotNull { loadSession(it.id) } }

    /** Completed games, newest first — used by the history screen (PRD F-035/F-036). */
    fun observeCompletedSessions(): Flow<List<GameSession>> =
        gameDao.observeCompletedSessions().map { list -> list.mapNotNull { loadSession(it.id) } }

    suspend fun getActiveSessions(): List<GameSession> =
        gameDao.getActiveSessions().mapNotNull { loadSession(it.id) }

    // ── Remembered player names ────────────────────────────────
    fun observeRecentNames(): Flow<List<String>> = nameHistoryDao.observeRecent()

    suspend fun rememberNames(names: List<String>) {
        val now = System.currentTimeMillis()
        val entities = names.map { it.trim() }.filter { it.isNotBlank() }.distinct()
            .map { RememberedNameEntity(it, now) }
        if (entities.isNotEmpty()) nameHistoryDao.upsertAll(entities)
    }

    // ── Create / load / delete ─────────────────────────────────
    /** Persists a freshly configured game and all of its child rows. */
    suspend fun createSession(session: GameSession) {
        gameDao.upsertSession(session.toSessionEntity())
        gameDao.upsertSettings(session.settings.toEntity(session.id))
        gameDao.upsertPlayers(session.players.map { it.toEntity(session.id) })
        gameDao.upsertHoleConfigs(session.holes.map { it.toEntity(session.id) })
    }

    suspend fun loadSession(id: String): GameSession? {
        val sessionEntity = gameDao.getSession(id) ?: return null
        val settings = gameDao.getSettings(id)?.toModel() ?: return null
        return GameSession(
            id = sessionEntity.id,
            createdAt = sessionEntity.createdAt,
            lastSavedAt = sessionEntity.lastSavedAt,
            players = gameDao.getPlayers(id).map { it.toModel() },
            holes = gameDao.getHoleConfigs(id).map { it.toModel() },
            scores = gameDao.getHoleScores(id).map { it.toModel() },
            penalties = gameDao.getPenaltyRecords(id).map { it.toModel() },
            settings = settings,
            state = sessionEntity.toState()
        )
    }

    suspend fun deleteSession(id: String) = gameDao.deleteSession(id)

    // ── Incremental auto-save ──────────────────────────────────
    /** Saves a single hole's score (confirmed or temporary). */
    suspend fun saveHoleScore(sessionId: String, score: HoleScore, state: GameState) {
        gameDao.upsertHoleScore(score.toEntity(sessionId))
        touchState(sessionId, state)
    }

    /** Replaces all penalty records for a hole atomically. */
    suspend fun savePenaltiesForHole(
        sessionId: String,
        holeNumber: Int,
        records: List<PenaltyRecord>,
        state: GameState
    ) {
        gameDao.deletePenaltiesForHole(sessionId, holeNumber)
        if (records.isNotEmpty()) {
            gameDao.upsertPenaltyRecords(records.map { it.toEntity(sessionId) })
        }
        touchState(sessionId, state)
    }

    /** Persists the live game state (current hole, phase, partial scores). */
    suspend fun saveState(sessionId: String, state: GameState) = touchState(sessionId, state)

    private suspend fun touchState(sessionId: String, state: GameState) {
        val existing = gameDao.getSession(sessionId) ?: return
        gameDao.upsertSession(
            existing.copy(
                lastSavedAt = System.currentTimeMillis(),
                currentHole = state.currentHole,
                roundPhase = state.roundPhase.name,
                isCompleted = state.isCompleted,
                frontNineCompletedAt = state.frontNineCompletedAt,
                partialScores = state.partialScores
            )
        )
    }

    /** Rewrites the entire penalty set for a session (used by result-screen edits). */
    suspend fun replaceAllPenalties(sessionId: String, records: List<PenaltyRecord>) {
        gameDao.deleteAllPenalties(sessionId)
        if (records.isNotEmpty()) {
            gameDao.upsertPenaltyRecords(records.map { it.toEntity(sessionId) })
        }
    }

    // ── Score edits (result screen) ────────────────────────────
    suspend fun recordEdit(edit: ScoreEdit) = gameDao.insertScoreEdit(edit.toEntity())

    suspend fun getEdits(sessionId: String): List<ScoreEdit> =
        gameDao.getScoreEdits(sessionId).map { it.toModel() }
}

private fun GameSession.toSessionEntity() = GameSessionEntity(
    id = id,
    createdAt = createdAt,
    lastSavedAt = lastSavedAt,
    currentHole = state.currentHole,
    roundPhase = state.roundPhase.name,
    isCompleted = state.isCompleted,
    frontNineCompletedAt = state.frontNineCompletedAt,
    partialScores = state.partialScores
)
