package com.golfsupporter.ui.round

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.golfsupporter.util.RoundRules
import com.golfsupporter.util.ScoreLabel

@Composable
fun RoundScreen(
    onFrontNineComplete: (String) -> Unit,
    onRoundComplete: (String) -> Unit,
    onExit: () -> Unit,
    viewModel: RoundViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPenaltySheet by remember { mutableStateOf(false) }

    // Persist a snapshot when the app is backgrounded (onStop) — PRD F-042.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.saveSnapshot()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.nav.collect { event ->
            when (event) {
                is RoundNav.FrontNineComplete -> onFrontNineComplete(event.sessionId)
                is RoundNav.RoundComplete -> onRoundComplete(event.sessionId)
            }
        }
    }

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // ── Header ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Hole ${state.currentHole} / PAR ${state.par}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            state.weather?.let { w ->
                Text(
                    "${w.iconEmoji} ${w.temperatureC}°C  💨 ${w.windDirection} ${w.windSpeedMs.toInt()}m/s",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        RoundProgress(state)
        Spacer(Modifier.height(12.dp))
        Divider()
        Spacer(Modifier.height(12.dp))

        // ── Player cards ──
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            state.players.forEach { player ->
                val relative = state.scores[player.id] ?: 0
                val total = state.cumulative[player.id] ?: 0
                if (state.inputModeScroll) {
                    ScrollScoreCard(
                        name = player.name,
                        relative = relative,
                        total = total,
                        onChange = { delta -> viewModel.changeScore(player.id, delta) },
                    )
                } else {
                    ButtonScoreCard(
                        name = player.name,
                        relative = relative,
                        total = total,
                        onChange = { delta -> viewModel.changeScore(player.id, delta) },
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            if (state.penaltyEnabled) {
                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(8.dp))
                PenaltySummaryRow(state)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { showPenaltySheet = true }) {
                    Text("⚠️ 벌칙 입력")
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Divider()
        Spacer(Modifier.height(12.dp))

        // ── Footer navigation ──
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { viewModel.previousHole() },
                enabled = state.currentHole > state.firstHole,
                modifier = Modifier.weight(1f),
            ) { Text("← 이전 홀") }

            val isLast = state.currentHole == state.lastHole
            val isFrontEnd = state.currentHole == 9 && RoundRules.hasInterstitial(state.roundType)
            Button(
                onClick = { viewModel.nextHole() },
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    when {
                        isLast -> "결과 보기 →"
                        isFrontEnd -> "전반 완료 →"
                        else -> "다음 홀 →"
                    }
                )
            }
        }
    }

    if (showPenaltySheet) {
        PenaltyBottomSheet(
            state = state,
            onChange = { playerId, typeId, delta -> viewModel.changePenalty(playerId, typeId, delta) },
            onDismiss = { showPenaltySheet = false },
        )
    }
}

@Composable
private fun RoundProgress(state: RoundUiState) {
    val range = RoundRules.holeRange(state.roundType)
    val played = (state.currentHole - range.first).coerceAtLeast(0)
    val total = range.count()
    val frontLabel = if (RoundRules.isFrontNine(state.currentHole)) "전반" else "후반"
    Column {
        LinearProgressIndicator(
            progress = { (played.toFloat() + 1) / total },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "$frontLabel  ${state.currentHole - range.first + 1}/$total",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun PenaltySummaryRow(state: RoundUiState) {
    val parts = buildList {
        state.players.forEach { player ->
            val pens = state.penalties[player.id].orEmpty().filterValues { it > 0 }
            pens.forEach { (typeId, count) ->
                val type = state.activePenaltyTypes.firstOrNull { it.id == typeId }
                if (type != null) add("${type.emoji}${player.name}:${type.label}×$count")
            }
        }
    }
    Text(
        text = if (parts.isEmpty()) "이번 홀 벌칙 없음" else parts.joinToString("  "),
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
    )
}
