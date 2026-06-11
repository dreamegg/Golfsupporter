package com.golfsupporter.ui.round

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfsupporter.data.model.RoundPhase
import com.golfsupporter.data.repository.GameRepository
import com.golfsupporter.ui.home.PlayerStanding
import com.golfsupporter.ui.navigation.Routes
import com.golfsupporter.util.ScoreLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PenaltyLine(val playerName: String, val summary: String)

data class InterstitialUiState(
    val loading: Boolean = true,
    val standings: List<PlayerStanding> = emptyList(),
    val penaltyLines: List<PenaltyLine> = emptyList(),
)

@HiltViewModel
class InterstitialViewModel @Inject constructor(
    private val repository: GameRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle[Routes.ARG_SESSION_ID])

    private val _uiState = MutableStateFlow(InterstitialUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val session = repository.loadSession(sessionId) ?: return
        val penaltyTypes = repository.getPenaltyTypes().associateBy { it.id }

        val frontScores = session.scores.filter { it.holeNumber in 1..9 }
        val standings = session.players.map { player ->
            val total = frontScores.sumOf { it.playerScores[player.id] ?: 0 }
            PlayerStanding(player.name, total, ScoreLabel.formatTotal(total))
        }.sortedBy { it.total }

        val penaltyLines = session.players.mapNotNull { player ->
            val byType = session.penalties
                .filter { it.holeNumber in 1..9 && it.playerId == player.id }
                .groupBy { it.penaltyTypeId }
                .mapValues { entry -> entry.value.sumOf { it.count } }
                .filterValues { it > 0 }
            if (byType.isEmpty()) null
            else PenaltyLine(
                player.name,
                byType.entries.joinToString("  ") { (id, count) ->
                    val type = penaltyTypes[id]
                    "${type?.emoji ?: ""}${type?.label ?: id}×$count"
                }
            )
        }

        _uiState.value = InterstitialUiState(false, standings, penaltyLines)
    }

    fun startBackNine(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val session = repository.loadSession(sessionId) ?: return@launch
            repository.saveState(
                sessionId,
                session.state.copy(currentHole = 10, roundPhase = RoundPhase.BACK_NINE)
            )
            onReady(sessionId)
        }
    }
}
