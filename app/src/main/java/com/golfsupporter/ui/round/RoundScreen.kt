package com.golfsupporter.ui.round

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
                PenaltyInlineSection(
                    state = state,
                    onChange = { playerId, typeId, delta -> viewModel.changePenalty(playerId, typeId, delta) },
                )
            }

            if (state.holeScores.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(12.dp))
                Scoreboard(state)
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

}

/**
 * Inline penalty entry: tap a player's name to expand a panel of penalty types,
 * then use −/+ to record penalties for that specific player. Each player expands
 * independently; collapsed rows show a summary of what they already have.
 */
@Composable
private fun PenaltyInlineSection(
    state: RoundUiState,
    onChange: (playerId: Int, typeId: String, delta: Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(emptySet<Int>()) }

    Text("⚠️ 벌칙 입력", fontWeight = FontWeight.Bold, fontSize = 15.sp)
    Spacer(Modifier.height(4.dp))
    Text(
        "이름을 탭하면 벌칙 항목이 펼쳐집니다",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    )
    Spacer(Modifier.height(8.dp))

    state.players.forEach { player ->
        val isOpen = player.id in expanded
        val playerPenalties = state.penalties[player.id].orEmpty().filterValues { it > 0 }
        PlayerPenaltyItem(
            player = player,
            isOpen = isOpen,
            playerPenalties = playerPenalties,
            activeTypes = state.activePenaltyTypes,
            onToggle = {
                expanded = if (isOpen) expanded - player.id else expanded + player.id
            },
            onChange = { typeId, delta -> onChange(player.id, typeId, delta) },
        )
        Divider()
    }
}

@Composable
private fun PlayerPenaltyItem(
    player: com.golfsupporter.data.model.Player,
    isOpen: Boolean,
    playerPenalties: Map<String, Int>,
    activeTypes: List<com.golfsupporter.data.model.PenaltyType>,
    onToggle: () -> Unit,
    onChange: (typeId: String, delta: Int) -> Unit,
) {
    // Header — tap to expand/collapse.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(player.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.width(10.dp))
        val summary = activeTypes
            .mapNotNull { t -> playerPenalties[t.id]?.let { "${t.emoji}×$it" } }
            .joinToString("  ")
        Text(
            summary.ifEmpty { "벌칙 없음" },
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(
                alpha = if (summary.isEmpty()) 0.45f else 0.85f
            ),
        )
        Icon(
            if (isOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = if (isOpen) "접기" else "펼치기",
        )
    }

    // Expanded — choose which penalty to record for this player.
    if (isOpen) {
        activeTypes.forEach { type ->
            val count = playerPenalties[type.id] ?: 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${type.emoji} ${type.label}", modifier = Modifier.weight(1f), fontSize = 15.sp)
                OutlinedIconButton(
                    onClick = { onChange(type.id, -1) },
                    enabled = count > 0,
                    modifier = Modifier.size(40.dp),
                ) { Text("−", fontSize = 20.sp) }
                Text(
                    "$count",
                    modifier = Modifier.width(36.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                OutlinedIconButton(
                    onClick = { onChange(type.id, 1) },
                    modifier = Modifier.size(40.dp),
                ) { Text("+", fontSize = 20.sp) }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun RoundProgress(state: RoundUiState) {
    val range = RoundRules.holeRange(state.roundType)
    val total = range.count()
    val overallPos = (state.currentHole - range.first + 1).coerceIn(1, total)

    // Segment (front/back nine) position within its own nine.
    val isFront = RoundRules.isFrontNine(state.currentHole)
    val segLabel = if (isFront) "전반" else "후반"
    val segStart = if (isFront) 1 else 10
    val segPos = state.currentHole - segStart + 1

    Column {
        Row {
            Text(
                "$segLabel $segPos/9",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                modifier = Modifier.weight(1f),
            )
            Text(
                "전체 $overallPos/$total",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { overallPos.toFloat() / total },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
        )
    }
}

/**
 * Live scorecard table: previous holes' per-player scores plus the running
 * total shot count, so players can monitor progress during input. The hole
 * columns scroll horizontally while the player labels and total stay pinned.
 */
@Composable
private fun Scoreboard(state: RoundUiState) {
    val holes = state.holeScores.keys.sorted()
    if (holes.isEmpty()) return

    val labelW = 64.dp
    val holeW = 34.dp
    val totalW = 52.dp
    val headerBg = MaterialTheme.colorScheme.surfaceVariant
    val currentBg = MaterialTheme.colorScheme.primaryContainer
    val totalPar = state.holePars.values.sum()

    Text("📊 스코어보드", fontWeight = FontWeight.Bold, fontSize = 14.sp)
    Spacer(Modifier.height(6.dp))
    Row {
        // ── Pinned label column ──
        Column {
            ScoreCell("홀", labelW, bg = headerBg, bold = true)
            ScoreCell("PAR", labelW, bg = headerBg)
            state.players.forEach { p ->
                ScoreCell(p.name, labelW, bold = true, align = TextOverflow.Ellipsis)
            }
        }

        // ── Scrollable hole columns ──
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
        ) {
            holes.forEach { hole ->
                val isCurrent = hole == state.currentHole
                val par = state.holePars[hole] ?: 0
                Column {
                    ScoreCell("$hole", holeW, bg = if (isCurrent) currentBg else headerBg, bold = true)
                    ScoreCell("$par", holeW, bg = if (isCurrent) currentBg else headerBg)
                    state.players.forEach { p ->
                        val relative = state.holeScores[hole]?.get(p.id) ?: 0
                        ScoreCell(
                            text = "${par + relative}",
                            width = holeW,
                            color = ScoreLabel.colorFor(relative),
                            bg = if (isCurrent) currentBg.copy(alpha = 0.4f) else Color.Transparent,
                        )
                    }
                }
            }
        }

        // ── Pinned total (shot count) column ──
        Column {
            ScoreCell("합계", totalW, bg = headerBg, bold = true)
            ScoreCell("$totalPar", totalW, bg = headerBg)
            state.players.forEach { p ->
                val shots = state.totalShots[p.id] ?: 0
                val rel = state.cumulative[p.id] ?: 0
                ScoreCell(
                    text = "$shots",
                    width = totalW,
                    color = ScoreLabel.colorFor(rel),
                    bold = true,
                )
            }
        }
    }
}

@Composable
private fun ScoreCell(
    text: String,
    width: Dp,
    color: Color = Color.Unspecified,
    bold: Boolean = false,
    bg: Color = Color.Transparent,
    align: TextOverflow = TextOverflow.Clip,
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(28.dp)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = color,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = align,
        )
    }
}
