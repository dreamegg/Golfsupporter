package com.golfsupporter.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfsupporter.data.model.GameSession
import com.golfsupporter.data.model.GameSettings
import com.golfsupporter.data.model.GameState
import com.golfsupporter.data.model.HoleConfig
import com.golfsupporter.data.model.PenaltyType
import com.golfsupporter.data.model.Player
import com.golfsupporter.data.model.RoundPhase
import com.golfsupporter.data.model.RoundType
import com.golfsupporter.data.model.ScoreInputMode
import com.golfsupporter.data.repository.GameRepository
import com.golfsupporter.util.RoundRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class SetupUiState(
    val step: Int = 1,
    val playerCount: Int = 2,
    val playerNames: List<String> = listOf("", "", "", ""),
    val pars: Map<Int, Int> = (1..18).associateWith { 4 },
    val roundType: RoundType = RoundType.FULL_18,
    val scoreInputMode: ScoreInputMode = ScoreInputMode.BUTTON,
    val penaltyEnabled: Boolean = true,
    val penaltyTypes: List<PenaltyType> = emptyList(),
    val activePenaltyIds: Set<String> = emptySet(),
) {
    val activeHoles: List<Int> get() = RoundRules.holeRange(roundType).toList()
}

const val MAX_NAME_LENGTH = 8
const val MAX_CUSTOM_PENALTIES = 20

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val repository: GameRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observePenaltyTypes().collect { types ->
                _uiState.update { current ->
                    // Default active selection = the built-in default-enabled set,
                    // unless the user has already made a choice.
                    val active = if (current.activePenaltyIds.isEmpty() && current.penaltyTypes.isEmpty()) {
                        types.filter { it.isEnabledByDefault }.map { it.id }.toSet()
                    } else current.activePenaltyIds
                    current.copy(penaltyTypes = types, activePenaltyIds = active)
                }
            }
        }
    }

    // ── Navigation between steps ───────────────────────────────
    fun goToStep(step: Int) = _uiState.update { it.copy(step = step.coerceIn(1, 3)) }
    fun nextStep() = goToStep(_uiState.value.step + 1)
    fun previousStep() = goToStep(_uiState.value.step - 1)

    // ── Step 1: players ────────────────────────────────────────
    fun setPlayerCount(count: Int) = _uiState.update { it.copy(playerCount = count) }

    fun setPlayerName(index: Int, name: String) = _uiState.update { state ->
        val trimmed = name.take(MAX_NAME_LENGTH)
        state.copy(playerNames = state.playerNames.toMutableList().apply { this[index] = trimmed })
    }

    // ── Step 2: pars ───────────────────────────────────────────
    fun setAllPars(par: Int) = _uiState.update { state ->
        state.copy(pars = state.pars.mapValues { par })
    }

    fun setPar(hole: Int, par: Int) = _uiState.update { state ->
        state.copy(pars = state.pars.toMutableMap().apply { this[hole] = par })
    }

    // ── Step 3: options ────────────────────────────────────────
    fun setRoundType(type: RoundType) = _uiState.update { it.copy(roundType = type) }
    fun setInputMode(mode: ScoreInputMode) = _uiState.update { it.copy(scoreInputMode = mode) }
    fun setPenaltyEnabled(enabled: Boolean) = _uiState.update { it.copy(penaltyEnabled = enabled) }

    fun togglePenalty(id: String) = _uiState.update { state ->
        val active = state.activePenaltyIds.toMutableSet()
        if (!active.add(id)) active.remove(id)
        state.copy(activePenaltyIds = active)
    }

    fun addCustomPenalty(label: String, emoji: String) {
        val state = _uiState.value
        val customCount = state.penaltyTypes.count { it.isCustom }
        if (label.isBlank() || customCount >= MAX_CUSTOM_PENALTIES) return
        val id = "CUSTOM_${UUID.randomUUID()}"
        val sortOrder = (state.penaltyTypes.maxOfOrNull { it.sortOrder } ?: 0) + 1
        viewModelScope.launch {
            repository.addCustomPenalty(
                PenaltyType(
                    id = id,
                    label = label.take(12),
                    emoji = emoji.ifBlank { "⛳" },
                    isCustom = true,
                    isEnabledByDefault = false,
                    sortOrder = sortOrder,
                )
            )
            togglePenalty(id) // auto-select newly added custom items
        }
    }

    fun deleteCustomPenalty(id: String) {
        viewModelScope.launch { repository.deleteCustomPenalty(id) }
        _uiState.update { it.copy(activePenaltyIds = it.activePenaltyIds - id) }
    }

    // ── Validation ─────────────────────────────────────────────
    fun playersValid(): Boolean {
        val s = _uiState.value
        return (0 until s.playerCount).all { s.playerNames[it].isNotBlank() }
    }

    // ── Build & persist the session ────────────────────────────
    fun createGame(onCreated: (String) -> Unit) {
        val s = _uiState.value
        val players = (0 until s.playerCount).map { Player(id = it, name = s.playerNames[it].trim()) }
        val holes = s.activeHoles.map { HoleConfig(holeNumber = it, par = s.pars[it] ?: 4) }

        val startHole = RoundRules.firstHole(s.roundType)
        val initialPhase =
            if (s.roundType == RoundType.BACK_9) RoundPhase.BACK_NINE else RoundPhase.FRONT_NINE

        val now = System.currentTimeMillis()
        val session = GameSession(
            id = UUID.randomUUID().toString(),
            createdAt = now,
            lastSavedAt = now,
            players = players,
            holes = holes,
            scores = emptyList(),
            penalties = emptyList(),
            settings = GameSettings(
                roundType = s.roundType,
                scoreInputMode = s.scoreInputMode,
                penaltyEnabled = s.penaltyEnabled,
                activePenaltyIds = s.activePenaltyIds.toList(),
            ),
            state = GameState(
                currentHole = startHole,
                roundPhase = initialPhase,
                isCompleted = false,
                frontNineCompletedAt = null,
                partialScores = null,
            )
        )

        viewModelScope.launch {
            repository.createSession(session)
            onCreated(session.id)
        }
    }
}
