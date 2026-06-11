package com.golfsupporter.ui.round

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfsupporter.data.model.GameSession
import com.golfsupporter.data.model.HoleScore
import com.golfsupporter.data.model.PenaltyRecord
import com.golfsupporter.data.model.PenaltyType
import com.golfsupporter.data.model.Player
import com.golfsupporter.data.model.RoundPhase
import com.golfsupporter.data.model.RoundType
import com.golfsupporter.data.location.LatLng
import com.golfsupporter.data.repository.GameRepository
import com.golfsupporter.data.weather.Weather
import com.golfsupporter.data.weather.WeatherRepository
import com.golfsupporter.ui.navigation.Routes
import com.golfsupporter.util.RoundRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoundUiState(
    val loading: Boolean = true,
    val sessionId: String = "",
    val players: List<Player> = emptyList(),
    val roundType: RoundType = RoundType.FULL_18,
    val inputModeScroll: Boolean = false,
    val penaltyEnabled: Boolean = false,
    val activePenaltyTypes: List<PenaltyType> = emptyList(),
    val currentHole: Int = 1,
    val par: Int = 4,
    val phase: RoundPhase = RoundPhase.FRONT_NINE,
    val scores: Map<Int, Int> = emptyMap(),          // playerId -> relative for current hole
    val cumulative: Map<Int, Int> = emptyMap(),       // playerId -> running total
    val penalties: Map<Int, Map<String, Int>> = emptyMap(), // playerId -> (typeId -> count) for current hole
    val firstHole: Int = 1,
    val lastHole: Int = 18,
    val weather: Weather? = null,
)

sealed interface RoundNav {
    data class FrontNineComplete(val sessionId: String) : RoundNav
    data class RoundComplete(val sessionId: String) : RoundNav
}

@HiltViewModel
class RoundViewModel @Inject constructor(
    private val repository: GameRepository,
    private val weatherRepository: WeatherRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle[Routes.ARG_SESSION_ID])

    private val _uiState = MutableStateFlow(RoundUiState())
    val uiState = _uiState.asStateFlow()

    private val _nav = MutableSharedFlow<RoundNav>(extraBufferCapacity = 1)
    val nav = _nav.asSharedFlow()

    private lateinit var session: GameSession
    private val scoresByHole = mutableMapOf<Int, MutableMap<Int, Int>>()
    private val penaltiesByHole = mutableMapOf<Int, MutableMap<Pair<Int, String>, Int>>()
    private var cachedPenaltyTypes: List<PenaltyType> = emptyList()

    init {
        viewModelScope.launch {
            cachedPenaltyTypes = repository.getPenaltyTypes()
            load()
        }
    }

    private suspend fun load() {
        val loaded = repository.loadSession(sessionId) ?: return
        session = loaded

        // Rebuild in-memory working state from persisted rows.
        loaded.scores.forEach { hs ->
            scoresByHole[hs.holeNumber] = hs.playerScores.toMutableMap()
        }
        loaded.penalties.forEach { pr ->
            val holeMap = penaltiesByHole.getOrPut(pr.holeNumber) { mutableMapOf() }
            holeMap[pr.playerId to pr.penaltyTypeId] = pr.count
        }

        var phase = loaded.state.roundPhase
        var currentHole = loaded.state.currentHole

        // Resuming a SPLIT/FULL game whose front nine is done: jump to the back nine.
        if (phase == RoundPhase.FRONT_COMPLETED) {
            phase = RoundPhase.BACK_NINE
            currentHole = 10
            persistState(currentHole, phase, loaded.state.frontNineCompletedAt)
        }

        ensureHoleInitialised(currentHole)
        publish(currentHole, phase)
        _uiState.update { it.copy(loading = false) }

        startWeatherUpdates()
    }

    /**
     * Periodically refreshes weather for the course location while the round is
     * open (PRD G-006/G-007/G-008). Does nothing if no course was selected or no
     * weather API key is configured (offline fallback, G-009).
     */
    private fun startWeatherUpdates() {
        val lat = session.settings.courseLatitude ?: return
        val lon = session.settings.courseLongitude ?: return
        if (!weatherRepository.isEnabled) return
        val coords = LatLng(lat, lon)
        viewModelScope.launch {
            while (isActive) {
                val weather = weatherRepository.fetch(coords)
                if (weather != null) _uiState.update { it.copy(weather = weather) }
                delay(Weather.REFRESH_INTERVAL_MS)
            }
        }
    }

    private fun ensureHoleInitialised(hole: Int) {
        if (scoresByHole[hole] == null) {
            scoresByHole[hole] = session.players.associate { it.id to 0 }.toMutableMap()
        }
    }

    private fun parFor(hole: Int): Int =
        session.holes.firstOrNull { it.holeNumber == hole }?.par ?: 4

    private fun publish(hole: Int, phase: RoundPhase) {
        val activeTypes = session.settings.activePenaltyIds.let { active ->
            // resolved against the global catalogue lazily in load(); fetched here
            cachedPenaltyTypes.filter { it.id in active }
        }
        val cumulative = session.players.associate { player ->
            player.id to scoresByHole.entries.sumOf { (_, m) -> m[player.id] ?: 0 }
        }
        val holePenalties = session.players.associate { player ->
            player.id to (penaltiesByHole[hole].orEmpty()
                .filterKeys { it.first == player.id }
                .mapKeys { it.key.second })
        }
        _uiState.update {
            it.copy(
                sessionId = sessionId,
                players = session.players,
                roundType = session.settings.roundType,
                inputModeScroll = session.settings.scoreInputMode == com.golfsupporter.data.model.ScoreInputMode.SCROLL,
                penaltyEnabled = session.settings.penaltyEnabled,
                activePenaltyTypes = activeTypes,
                currentHole = hole,
                par = parFor(hole),
                phase = phase,
                scores = scoresByHole[hole].orEmpty().toMap(),
                cumulative = cumulative,
                penalties = holePenalties,
                firstHole = RoundRules.firstHole(session.settings.roundType),
                lastHole = RoundRules.lastHole(session.settings.roundType),
            )
        }
    }

    // ── Score editing ──────────────────────────────────────────
    fun changeScore(playerId: Int, delta: Int) {
        val hole = _uiState.value.currentHole
        val map = scoresByHole.getOrPut(hole) { mutableMapOf() }
        val par = parFor(hole)
        val next = RoundRules.clampRelative(par, (map[playerId] ?: 0) + delta)
        map[playerId] = next
        publish(hole, _uiState.value.phase)
        autoSaveCurrentHole(confirmed = false)
    }

    fun setScore(playerId: Int, relative: Int) {
        val hole = _uiState.value.currentHole
        val map = scoresByHole.getOrPut(hole) { mutableMapOf() }
        map[playerId] = RoundRules.clampRelative(parFor(hole), relative)
        publish(hole, _uiState.value.phase)
        autoSaveCurrentHole(confirmed = false)
    }

    // ── Penalty editing ────────────────────────────────────────
    fun changePenalty(playerId: Int, typeId: String, delta: Int) {
        if (!_uiState.value.penaltyEnabled) return
        val hole = _uiState.value.currentHole
        val map = penaltiesByHole.getOrPut(hole) { mutableMapOf() }
        val key = playerId to typeId
        val next = ((map[key] ?: 0) + delta).coerceAtLeast(0)
        if (next == 0) map.remove(key) else map[key] = next
        publish(hole, _uiState.value.phase)
        savePenalties(hole)
    }

    // ── Navigation between holes ───────────────────────────────
    fun nextHole() {
        val hole = _uiState.value.currentHole
        val type = session.settings.roundType

        // Resolve the transition target first, then do a single ordered write of
        // the confirmed hole score + new state (avoids racing two save coroutines).
        data class Target(val hole: Int, val phase: RoundPhase, val frontAt: Long?, val completed: Boolean, val nav: RoundNav?)

        val target = when {
            hole == 9 && RoundRules.hasInterstitial(type) ->
                Target(hole, RoundPhase.FRONT_COMPLETED, System.currentTimeMillis(), false, RoundNav.FrontNineComplete(sessionId))
            hole == RoundRules.lastHole(type) ->
                Target(hole, RoundPhase.COMPLETED, session.state.frontNineCompletedAt, true, RoundNav.RoundComplete(sessionId))
            else -> {
                val next = hole + 1
                val phase = if (RoundRules.isBackNine(next)) RoundPhase.BACK_NINE else _uiState.value.phase
                Target(next, phase, session.state.frontNineCompletedAt, false, null)
            }
        }

        session = session.copy(
            state = session.state.copy(
                currentHole = target.hole,
                roundPhase = target.phase,
                isCompleted = target.completed,
                frontNineCompletedAt = target.frontAt,
            )
        )

        val playerScores = scoresByHole[hole]?.toMap().orEmpty()
        viewModelScope.launch {
            repository.saveHoleScore(
                sessionId,
                HoleScore(hole, playerScores, isConfirmed = true),
                session.state
            )
        }

        if (target.nav != null) {
            _uiState.update { it.copy(phase = target.phase) }
            _nav.tryEmit(target.nav)
        } else {
            ensureHoleInitialised(target.hole)
            publish(target.hole, target.phase)
        }
    }

    fun previousHole() {
        val hole = _uiState.value.currentHole
        if (hole <= _uiState.value.firstHole) return
        val prev = hole - 1
        val phase = if (RoundRules.isFrontNine(prev) && _uiState.value.phase == RoundPhase.BACK_NINE)
            RoundPhase.FRONT_NINE else _uiState.value.phase
        ensureHoleInitialised(prev)
        persistState(prev, phase, session.state.frontNineCompletedAt)
        publish(prev, phase)
    }

    /** Saves a snapshot of the live state when the app goes to background (onStop). */
    fun saveSnapshot() {
        if (!::session.isInitialized) return
        autoSaveCurrentHole(confirmed = false)
    }

    // ── Persistence helpers ────────────────────────────────────
    private fun autoSaveCurrentHole(confirmed: Boolean) {
        val hole = _uiState.value.currentHole
        val playerScores = scoresByHole[hole]?.toMap() ?: return
        val state = currentState(hole, _uiState.value.phase)
        viewModelScope.launch {
            repository.saveHoleScore(
                sessionId,
                HoleScore(hole, playerScores, isConfirmed = confirmed),
                state
            )
        }
    }

    private fun savePenalties(hole: Int) {
        val records = penaltiesByHole[hole].orEmpty().map { (key, count) ->
            PenaltyRecord(hole, key.first, key.second, count)
        }
        val state = currentState(hole, _uiState.value.phase)
        viewModelScope.launch {
            repository.savePenaltiesForHole(sessionId, hole, records, state)
        }
    }

    private fun persistState(
        hole: Int,
        phase: RoundPhase,
        frontCompletedAt: Long?,
        completed: Boolean = false,
    ) {
        session = session.copy(
            state = session.state.copy(
                currentHole = hole,
                roundPhase = phase,
                isCompleted = completed,
                frontNineCompletedAt = frontCompletedAt,
            )
        )
        viewModelScope.launch { repository.saveState(sessionId, session.state) }
    }

    private fun currentState(hole: Int, phase: RoundPhase) = session.state.copy(
        currentHole = hole,
        roundPhase = phase,
    )
}
