package com.golfsupporter.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfsupporter.data.model.GameSession
import com.golfsupporter.data.model.RoundPhase
import com.golfsupporter.data.repository.GameRepository
import com.golfsupporter.util.ScoreLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerStanding(val name: String, val total: Int, val totalLabel: String)

data class ActiveGameBanner(
    val sessionId: String,
    val playerNames: String,
    val currentHole: Int,
    val lastHole: Int,
    val phase: RoundPhase,
    val standings: List<PlayerStanding>,
)

data class HomeUiState(
    val banner: ActiveGameBanner? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: GameRepository,
) : ViewModel() {

    val uiState = repository.observeActiveSession()
        .map { session -> HomeUiState(banner = session?.toBanner()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** Wipes all saved games, history and remembered names. */
    fun resetAllData() {
        viewModelScope.launch { repository.resetAllData() }
    }

    /** Marks the active game as completed so it leaves the continue banner (F-046). */
    fun discardActive(sessionId: String) {
        viewModelScope.launch {
            val session = repository.loadSession(sessionId) ?: return@launch
            repository.saveState(
                sessionId,
                session.state.copy(isCompleted = true, roundPhase = RoundPhase.COMPLETED)
            )
        }
    }
}

private fun GameSession.toBanner(): ActiveGameBanner {
    val standings = players.map { player ->
        val total = scores
            .filter { it.isConfirmed }
            .sumOf { it.playerScores[player.id] ?: 0 }
        PlayerStanding(player.name, total, ScoreLabel.formatTotal(total))
    }.sortedBy { it.total }

    return ActiveGameBanner(
        sessionId = id,
        playerNames = players.joinToString(" / ") { it.name },
        currentHole = state.currentHole,
        lastHole = com.golfsupporter.util.RoundRules.lastHole(settings.roundType),
        phase = state.roundPhase,
        standings = standings,
    )
}
