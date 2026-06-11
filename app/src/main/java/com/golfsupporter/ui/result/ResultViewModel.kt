package com.golfsupporter.ui.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfsupporter.data.model.EditType
import com.golfsupporter.data.model.HoleConfig
import com.golfsupporter.data.model.HoleScore
import com.golfsupporter.data.model.PenaltyRecord
import com.golfsupporter.data.model.PenaltyType
import com.golfsupporter.data.model.Player
import com.golfsupporter.data.model.ScoreEdit
import com.golfsupporter.data.repository.GameRepository
import com.golfsupporter.ui.navigation.Routes
import com.golfsupporter.util.ScoreLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class PlayerTotals(
    val player: Player,
    val front: Int,
    val back: Int,
    val total: Int,
    val rank: Int,
)

data class ResultUiState(
    val loading: Boolean = true,
    val players: List<Player> = emptyList(),
    val holes: List<HoleConfig> = emptyList(),
    val scoreCells: Map<Int, Map<Int, Int>> = emptyMap(),     // playerId -> (hole -> relative)
    val totals: List<PlayerTotals> = emptyList(),
    val penaltyEnabled: Boolean = false,
    val penaltyTypes: List<PenaltyType> = emptyList(),
    val penaltyCounts: Map<Int, Map<String, Int>> = emptyMap(), // playerId -> (typeId -> count)
    val editMode: Boolean = false,
    val editedScoreCells: Set<Pair<Int, Int>> = emptySet(),     // (playerId, hole)
    val editedPenaltyCells: Set<Pair<Int, String>> = emptySet(),// (playerId, typeId)
) {
    val hasFrontAndBack: Boolean
        get() = holes.any { it.holeNumber in 1..9 } && holes.any { it.holeNumber in 10..18 }
}

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val repository: GameRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle[Routes.ARG_SESSION_ID])

    private val _uiState = MutableStateFlow(ResultUiState())
    val uiState = _uiState.asStateFlow()

    // Working copies (edited in place during edit mode).
    private val scores = mutableMapOf<Int, MutableMap<Int, Int>>()           // hole -> playerId -> rel
    private val penaltyAgg = mutableMapOf<Pair<Int, String>, Int>()          // (player,type) -> count
    private var originalPenaltyRecords: List<PenaltyRecord> = emptyList()

    // Snapshots taken when entering edit mode (for rollback + edit history).
    private var snapshotScores: Map<Int, Map<Int, Int>> = emptyMap()
    private var snapshotPenalty: Map<Pair<Int, String>, Int> = emptyMap()

    private var firstHole: Int = 1

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val session = repository.loadSession(sessionId) ?: return
        firstHole = session.holes.minOfOrNull { it.holeNumber } ?: 1

        session.scores.forEach { hs ->
            scores[hs.holeNumber] = hs.playerScores.toMutableMap()
        }
        originalPenaltyRecords = session.penalties
        session.penalties.forEach { pr ->
            val key = pr.playerId to pr.penaltyTypeId
            penaltyAgg[key] = (penaltyAgg[key] ?: 0) + pr.count
        }

        val penaltyTypes = repository.getPenaltyTypes()
            .filter { it.id in session.settings.activePenaltyIds }

        _uiState.update {
            it.copy(
                loading = false,
                players = session.players,
                holes = session.holes.sortedBy { h -> h.holeNumber },
                penaltyEnabled = session.settings.penaltyEnabled,
                penaltyTypes = penaltyTypes,
            )
        }
        recompute()
    }

    private fun recompute() {
        val players = _uiState.value.players
        val holes = _uiState.value.holes

        val scoreCells = players.associate { player ->
            player.id to holes.associate { h ->
                h.holeNumber to (scores[h.holeNumber]?.get(player.id) ?: 0)
            }
        }

        val rawTotals = players.map { player ->
            val front = holes.filter { it.holeNumber in 1..9 }
                .sumOf { scores[it.holeNumber]?.get(player.id) ?: 0 }
            val back = holes.filter { it.holeNumber in 10..18 }
                .sumOf { scores[it.holeNumber]?.get(player.id) ?: 0 }
            Triple(player, front, back)
        }
        val sorted = rawTotals.sortedBy { it.second + it.third }
        val totals = rawTotals.map { (player, front, back) ->
            val total = front + back
            val rank = sorted.indexOfFirst { it.second + it.third == total } + 1
            PlayerTotals(player, front, back, total, rank)
        }

        val penaltyCounts = players.associate { player ->
            player.id to _uiState.value.penaltyTypes.associate { type ->
                type.id to (penaltyAgg[player.id to type.id] ?: 0)
            }.filterValues { it > 0 }
        }

        _uiState.update {
            it.copy(scoreCells = scoreCells, totals = totals, penaltyCounts = penaltyCounts)
        }
    }

    fun rankingText(): List<PlayerTotals> = _uiState.value.totals.sortedBy { it.rank }

    // ── Edit mode ──────────────────────────────────────────────
    fun enterEditMode() {
        snapshotScores = scores.mapValues { it.value.toMap() }
        snapshotPenalty = penaltyAgg.toMap()
        _uiState.update { it.copy(editMode = true, editedScoreCells = emptySet(), editedPenaltyCells = emptySet()) }
    }

    fun cancelEdit() {
        // Roll back working copies.
        scores.clear()
        snapshotScores.forEach { (hole, m) -> scores[hole] = m.toMutableMap() }
        penaltyAgg.clear()
        penaltyAgg.putAll(snapshotPenalty)
        _uiState.update { it.copy(editMode = false, editedScoreCells = emptySet(), editedPenaltyCells = emptySet()) }
        recompute()
    }

    fun editScore(playerId: Int, hole: Int, newRelative: Int) {
        val map = scores.getOrPut(hole) { mutableMapOf() }
        val clamped = com.golfsupporter.util.RoundRules.clampRelative(
            _uiState.value.holes.firstOrNull { it.holeNumber == hole }?.par ?: 4,
            newRelative
        )
        map[playerId] = clamped
        _uiState.update { it.copy(editedScoreCells = it.editedScoreCells + (playerId to hole)) }
        recompute()
    }

    fun editPenalty(playerId: Int, typeId: String, delta: Int) {
        val key = playerId to typeId
        penaltyAgg[key] = ((penaltyAgg[key] ?: 0) + delta).coerceAtLeast(0)
        _uiState.update { it.copy(editedPenaltyCells = it.editedPenaltyCells + key) }
        recompute()
    }

    fun saveEdits() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()

            // Persist changed scores + record edit history.
            _uiState.value.editedScoreCells.forEach { (playerId, hole) ->
                val original = snapshotScores[hole]?.get(playerId) ?: 0
                val updated = scores[hole]?.get(playerId) ?: 0
                if (original != updated) {
                    repository.recordEdit(
                        ScoreEdit(
                            id = UUID.randomUUID().toString(),
                            sessionId = sessionId,
                            editType = EditType.SCORE,
                            holeNumber = hole,
                            playerId = playerId,
                            originalValue = original,
                            newValue = updated,
                            editedAt = now,
                        )
                    )
                }
            }
            // Upsert all hole scores from the working copy.
            val state = repository.loadSession(sessionId)?.state ?: return@launch
            scores.forEach { (hole, playerScores) ->
                repository.saveHoleScore(
                    sessionId,
                    HoleScore(hole, playerScores.toMap(), isConfirmed = true),
                    state
                )
            }

            // Persist penalty changes (consolidated for edited pairs).
            val changedPairs = _uiState.value.editedPenaltyCells
            if (changedPairs.isNotEmpty()) {
                val newRecords = mutableListOf<PenaltyRecord>()
                originalPenaltyRecords
                    .filter { (it.playerId to it.penaltyTypeId) !in changedPairs }
                    .forEach { newRecords.add(it) }
                changedPairs.forEach { pair ->
                    val count = penaltyAgg[pair] ?: 0
                    if (count > 0) newRecords.add(PenaltyRecord(firstHole, pair.first, pair.second, count))
                    repository.recordEdit(
                        ScoreEdit(
                            id = UUID.randomUUID().toString(),
                            sessionId = sessionId,
                            editType = EditType.PENALTY,
                            holeNumber = firstHole,
                            playerId = pair.first,
                            penaltyTypeId = pair.second,
                            originalValue = snapshotPenalty[pair] ?: 0,
                            newValue = count,
                            editedAt = now,
                        )
                    )
                }
                repository.replaceAllPenalties(sessionId, newRecords)
                originalPenaltyRecords = newRecords
            }

            _uiState.update { it.copy(editMode = false, editedScoreCells = emptySet(), editedPenaltyCells = emptySet()) }
        }
    }

    fun shareText(): String {
        val s = _uiState.value
        val sb = StringBuilder("⛳ Golf Score Tracker\n\n🏆 최종 순위\n")
        rankingText().forEach { pt ->
            sb.append("${pt.rank}위 ${pt.player.name}  ${ScoreLabel.formatTotal(pt.total)}\n")
        }
        return sb.toString()
    }
}
