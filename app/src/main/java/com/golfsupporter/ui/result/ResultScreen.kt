package com.golfsupporter.ui.result

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.golfsupporter.data.model.PenaltyType
import com.golfsupporter.data.model.Player
import com.golfsupporter.util.ScoreLabel

@Composable
fun ResultScreen(
    onHome: () -> Unit,
    viewModel: ResultViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var scoreDialog by remember { mutableStateOf<Triple<Player, Int, Int>?>(null) } // player, hole, par

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // ── Top bar ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (state.editMode) "✏️ 수정 모드" else "🏆 최종 순위",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (state.editMode) {
                TextButton(onClick = { viewModel.cancelEdit() }) { Text("취소") }
                Button(onClick = { viewModel.saveEdits() }) { Text("저장") }
            } else {
                TextButton(onClick = { viewModel.enterEditMode() }) { Text("✏️ 수정") }
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, viewModel.shareText())
                    }
                    context.startActivity(Intent.createChooser(intent, "결과 공유"))
                }) { Text("공유") }
            }
        }

        Spacer(Modifier.height(8.dp))
        RankingRow(viewModel.rankingText())
        Spacer(Modifier.height(12.dp))

        if (state.editMode) {
            Text(
                "셀을 탭하여 수정하세요",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(8.dp))
        }

        val tabs = if (state.penaltyEnabled) listOf("스코어카드", "벌칙 요약") else listOf("스코어카드")
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { i, title ->
                Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(title) })
            }
        }
        Spacer(Modifier.height(12.dp))

        Box(Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> ScorecardTable(
                    state = state,
                    onCellTap = { player, hole, par ->
                        if (state.editMode) scoreDialog = Triple(player, hole, par)
                    },
                )
                1 -> PenaltyTable(
                    state = state,
                    onCellChange = { playerId, typeId, delta -> viewModel.editPenalty(playerId, typeId, delta) },
                )
            }
        }

        if (!state.editMode) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text("홈으로") }
        }
    }

    scoreDialog?.let { (player, hole, par) ->
        val current = state.scoreCells[player.id]?.get(hole) ?: 0
        ScoreEditDialog(
            player = player,
            hole = hole,
            par = par,
            current = current,
            onConfirm = { newRel ->
                viewModel.editScore(player.id, hole, newRel)
                scoreDialog = null
            },
            onDismiss = { scoreDialog = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RankingRow(ranking: List<PlayerTotals>) {
    val medals = listOf("🥇", "🥈", "🥉")
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ranking.forEachIndexed { i, pt ->
            Text(
                "${medals.getOrElse(i) { "${i + 1}." }} ${pt.player.name} ${ScoreLabel.formatTotal(pt.total)}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
private fun ScorecardTable(
    state: ResultUiState,
    onCellTap: (Player, Int, Int) -> Unit,
) {
    val frontHoles = state.holes.filter { it.holeNumber in 1..9 }
    val backHoles = state.holes.filter { it.holeNumber in 10..18 }
    val cellW = 40.dp

    Column(
        Modifier
            .fillMaxSize()
            .horizontalScroll(rememberScrollState())
    ) {
        // Header row
        Row {
            HeaderCell("", 64.dp)
            frontHoles.forEach { HeaderCell("H${it.holeNumber}", cellW) }
            if (frontHoles.isNotEmpty()) HeaderCell("전반", cellW)
            backHoles.forEach { HeaderCell("H${it.holeNumber}", cellW) }
            if (backHoles.isNotEmpty()) HeaderCell("후반", cellW)
            HeaderCell("합계", cellW)
        }
        Divider()
        Column(Modifier.verticalScroll(rememberScrollState())) {
            state.players.forEach { player ->
                val cells = state.scoreCells[player.id].orEmpty()
                val totals = state.totals.firstOrNull { it.player.id == player.id }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HeaderCell(player.name, 64.dp)
                    frontHoles.forEach { h ->
                        ScoreCell(
                            relative = cells[h.holeNumber] ?: 0,
                            width = cellW,
                            edited = (player.id to h.holeNumber) in state.editedScoreCells,
                            editable = state.editMode,
                            onTap = { onCellTap(player, h.holeNumber, h.par) },
                        )
                    }
                    if (frontHoles.isNotEmpty()) SubtotalCell(totals?.front ?: 0, cellW)
                    backHoles.forEach { h ->
                        ScoreCell(
                            relative = cells[h.holeNumber] ?: 0,
                            width = cellW,
                            edited = (player.id to h.holeNumber) in state.editedScoreCells,
                            editable = state.editMode,
                            onTap = { onCellTap(player, h.holeNumber, h.par) },
                        )
                    }
                    if (backHoles.isNotEmpty()) SubtotalCell(totals?.back ?: 0, cellW)
                    SubtotalCell(totals?.total ?: 0, cellW, bold = true)
                }
                Divider()
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text,
        modifier = Modifier
            .width(width)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ScoreCell(
    relative: Int,
    width: androidx.compose.ui.unit.Dp,
    edited: Boolean,
    editable: Boolean,
    onTap: () -> Unit,
) {
    val editedBg = Color(0xFFFFF59D).copy(alpha = 0.5f)
    Box(
        modifier = Modifier
            .width(width)
            .height(40.dp)
            .background(if (edited) editedBg else Color.Transparent)
            .let { if (editable) it.clickable { onTap() } else it }
            .then(if (edited) Modifier.underline(ScoreLabel.colorFor(relative)) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            ScoreLabel.shortLabel(relative),
            color = ScoreLabel.colorFor(relative),
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
        )
    }
}

private fun Modifier.underline(color: Color): Modifier = drawBehind {
    val y = size.height - 4f
    drawLine(color, Offset(8f, y), Offset(size.width - 8f, y), strokeWidth = 3f)
}

@Composable
private fun SubtotalCell(value: Int, width: androidx.compose.ui.unit.Dp, bold: Boolean = false) {
    Box(
        modifier = Modifier
            .width(width)
            .height(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            ScoreLabel.formatTotal(value),
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun PenaltyTable(
    state: ResultUiState,
    onCellChange: (playerId: Int, typeId: String, delta: Int) -> Unit,
) {
    val cellW = 56.dp
    Column(
        Modifier
            .fillMaxSize()
            .horizontalScroll(rememberScrollState())
    ) {
        Row {
            HeaderCell("", 64.dp)
            state.penaltyTypes.forEach { HeaderCell(it.emoji, cellW) }
            HeaderCell("합계", cellW)
        }
        Divider()
        state.players.forEach { player ->
            val counts = state.penaltyCounts[player.id].orEmpty()
            val rowTotal = counts.values.sum()
            Row(verticalAlignment = Alignment.CenterVertically) {
                HeaderCell(player.name, 64.dp)
                state.penaltyTypes.forEach { type ->
                    PenaltyCell(
                        count = counts[type.id] ?: 0,
                        width = cellW,
                        edited = (player.id to type.id) in state.editedPenaltyCells,
                        editable = state.editMode,
                        onIncrement = { onCellChange(player.id, type.id, 1) },
                        onDecrement = { onCellChange(player.id, type.id, -1) },
                    )
                }
                SubtotalCell(rowTotal, cellW, bold = true)
            }
            Divider()
        }
    }
}

@Composable
private fun PenaltyCell(
    count: Int,
    width: androidx.compose.ui.unit.Dp,
    edited: Boolean,
    editable: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    val editedBg = Color(0xFFFFF59D).copy(alpha = 0.5f)
    if (editable) {
        Row(
            modifier = Modifier
                .width(width)
                .height(40.dp)
                .background(if (edited) editedBg else Color.Transparent),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text("−", modifier = Modifier
                .clickable { onDecrement() }
                .padding(horizontal = 4.dp), fontSize = 16.sp)
            Text("$count", fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Text("+", modifier = Modifier
                .clickable { onIncrement() }
                .padding(horizontal = 4.dp), fontSize = 16.sp)
        }
    } else {
        Box(
            modifier = Modifier.width(width).height(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (count > 0) "$count" else "-", fontSize = 13.sp)
        }
    }
}

@Composable
private fun ScoreEditDialog(
    player: Player,
    hole: Int,
    par: Int,
    current: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableIntStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${player.name} — Hole $hole (PAR $par)") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(onClick = { value = com.golfsupporter.util.RoundRules.clampRelative(par, value - 1) }) { Text("−") }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(ScoreLabel.shortLabel(value), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = ScoreLabel.colorFor(value))
                        Text(ScoreLabel.nameFor(value), fontSize = 13.sp, color = ScoreLabel.colorFor(value))
                    }
                    OutlinedButton(onClick = { value = com.golfsupporter.util.RoundRules.clampRelative(par, value + 1) }) { Text("+") }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text("확인") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}
