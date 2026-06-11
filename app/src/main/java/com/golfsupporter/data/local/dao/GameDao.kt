package com.golfsupporter.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.golfsupporter.data.local.entity.GameSessionEntity
import com.golfsupporter.data.local.entity.GameSettingsEntity
import com.golfsupporter.data.local.entity.HoleConfigEntity
import com.golfsupporter.data.local.entity.HoleScoreEntity
import com.golfsupporter.data.local.entity.PenaltyRecordEntity
import com.golfsupporter.data.local.entity.PlayerEntity
import com.golfsupporter.data.local.entity.ScoreEditEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    // ── Session ────────────────────────────────────────────────
    @Upsert
    suspend fun upsertSession(session: GameSessionEntity)

    @Query("SELECT * FROM game_sessions WHERE id = :id")
    suspend fun getSession(id: String): GameSessionEntity?

    @Query("SELECT * FROM game_sessions ORDER BY createdAt DESC")
    fun observeAllSessions(): Flow<List<GameSessionEntity>>

    /** Most recent un-completed session, used for the home "continue" banner. */
    @Query("SELECT * FROM game_sessions WHERE isCompleted = 0 ORDER BY lastSavedAt DESC LIMIT 1")
    fun observeActiveSession(): Flow<GameSessionEntity?>

    @Query("SELECT * FROM game_sessions WHERE isCompleted = 0 ORDER BY lastSavedAt DESC")
    suspend fun getActiveSessions(): List<GameSessionEntity>

    @Query("DELETE FROM game_sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    // ── Settings ───────────────────────────────────────────────
    @Upsert
    suspend fun upsertSettings(settings: GameSettingsEntity)

    @Query("SELECT * FROM game_settings WHERE sessionId = :sessionId")
    suspend fun getSettings(sessionId: String): GameSettingsEntity?

    // ── Players ────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlayers(players: List<PlayerEntity>)

    @Query("SELECT * FROM players WHERE sessionId = :sessionId ORDER BY playerId")
    suspend fun getPlayers(sessionId: String): List<PlayerEntity>

    // ── Hole configs ───────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHoleConfigs(configs: List<HoleConfigEntity>)

    @Query("SELECT * FROM hole_configs WHERE sessionId = :sessionId ORDER BY holeNumber")
    suspend fun getHoleConfigs(sessionId: String): List<HoleConfigEntity>

    // ── Hole scores ────────────────────────────────────────────
    @Upsert
    suspend fun upsertHoleScore(score: HoleScoreEntity)

    @Query("SELECT * FROM hole_scores WHERE sessionId = :sessionId ORDER BY holeNumber")
    suspend fun getHoleScores(sessionId: String): List<HoleScoreEntity>

    // ── Penalty records ────────────────────────────────────────
    @Upsert
    suspend fun upsertPenaltyRecords(records: List<PenaltyRecordEntity>)

    @Query("DELETE FROM penalty_records WHERE sessionId = :sessionId AND holeNumber = :holeNumber")
    suspend fun deletePenaltiesForHole(sessionId: String, holeNumber: Int)

    @Query("DELETE FROM penalty_records WHERE sessionId = :sessionId")
    suspend fun deleteAllPenalties(sessionId: String)

    @Query("SELECT * FROM penalty_records WHERE sessionId = :sessionId")
    suspend fun getPenaltyRecords(sessionId: String): List<PenaltyRecordEntity>

    // ── Score edits ────────────────────────────────────────────
    @Insert
    suspend fun insertScoreEdit(edit: ScoreEditEntity)

    @Query("SELECT * FROM score_edits WHERE sessionId = :sessionId ORDER BY editedAt")
    suspend fun getScoreEdits(sessionId: String): List<ScoreEditEntity>
}
