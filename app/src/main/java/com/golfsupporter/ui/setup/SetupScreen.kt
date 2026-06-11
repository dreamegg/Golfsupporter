package com.golfsupporter.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.golfsupporter.data.model.RoundType
import com.golfsupporter.data.model.ScoreInputMode

@Composable
fun SetupScreen(
    onBack: () -> Unit,
    onGameCreated: (String) -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        StepIndicator(state.step)
        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            when (state.step) {
                1 -> StepPlayers(state, viewModel)
                2 -> StepPars(state, viewModel)
                3 -> StepOptions(state, viewModel)
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { if (state.step == 1) onBack() else viewModel.previousStep() },
                modifier = Modifier.weight(1f),
            ) { Text(if (state.step == 1) "취소" else "이전") }

            if (state.step < 3) {
                Button(
                    onClick = { viewModel.nextStep() },
                    enabled = state.step != 1 || viewModel.playersValid(),
                    modifier = Modifier.weight(1f),
                ) { Text("다음") }
            } else {
                Button(
                    onClick = { viewModel.createGame(onGameCreated) },
                    modifier = Modifier.weight(1f),
                ) { Text("게임 시작") }
            }
        }
    }
}

@Composable
private fun StepIndicator(step: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        (1..3).forEach { i ->
            androidx.compose.material3.LinearProgressIndicator(
                progress = { if (i <= step) 1f else 0f },
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp),
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    val title = when (step) {
        1 -> "STEP 1 · 플레이어"
        2 -> "STEP 2 · 코스 파 설정"
        else -> "STEP 3 · 게임 옵션"
    }
    Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
}

// ── Step 1 ─────────────────────────────────────────────────────
@Composable
private fun StepPlayers(state: SetupUiState, viewModel: SetupViewModel) {
    Text("인원 수", fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(2, 3, 4).forEach { count ->
            FilterChip(
                selected = state.playerCount == count,
                onClick = { viewModel.setPlayerCount(count) },
                label = { Text("${count}명") },
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    Text("이름 (최대 ${MAX_NAME_LENGTH}자)", fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    (0 until state.playerCount).forEach { i ->
        OutlinedTextField(
            value = state.playerNames[i],
            onValueChange = { viewModel.setPlayerName(i, it) },
            label = { Text("플레이어 ${i + 1}") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        )
    }
}

// ── Step 2 ─────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepPars(state: SetupUiState, viewModel: SetupViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(3, 4, 5).forEach { par ->
            OutlinedButton(onClick = { viewModel.setAllPars(par) }) {
                Text("전체 PAR $par")
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.activeHoles.forEach { hole ->
            HoleParToggle(
                hole = hole,
                par = state.pars[hole] ?: 4,
                onCycle = {
                    val current = state.pars[hole] ?: 4
                    val next = when (current) {
                        3 -> 4
                        4 -> 5
                        else -> 3
                    }
                    viewModel.setPar(hole, next)
                }
            )
        }
    }
}

@Composable
private fun HoleParToggle(hole: Int, par: Int, onCycle: () -> Unit) {
    OutlinedButton(
        onClick = onCycle,
        modifier = Modifier.width(72.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("H$hole", fontSize = 11.sp)
            Text("PAR $par", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

// ── Step 3 ─────────────────────────────────────────────────────
@Composable
private fun StepOptions(state: SetupUiState, viewModel: SetupViewModel) {
    var showCustomDialog by remember { mutableStateOf(false) }

    Text("라운드 타입", fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    val roundLabels = listOf(
        RoundType.FULL_18 to "18홀",
        RoundType.FRONT_9 to "전반",
        RoundType.BACK_9 to "후반",
        RoundType.SPLIT to "전/후반 분리",
    )
    FlowRowChips(roundLabels.map { it.second }, roundLabels.indexOfFirst { it.first == state.roundType }) { idx ->
        viewModel.setRoundType(roundLabels[idx].first)
    }

    Spacer(Modifier.height(16.dp))
    Text("스코어 입력 방식", fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    val modeLabels = listOf(ScoreInputMode.BUTTON to "버튼 모드", ScoreInputMode.SCROLL to "스크롤 모드")
    FlowRowChips(modeLabels.map { it.second }, modeLabels.indexOfFirst { it.first == state.scoreInputMode }) { idx ->
        viewModel.setInputMode(modeLabels[idx].first)
    }

    Spacer(Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("벌칙 시스템", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Switch(checked = state.penaltyEnabled, onCheckedChange = { viewModel.setPenaltyEnabled(it) })
    }

    if (state.penaltyEnabled) {
        Spacer(Modifier.height(8.dp))
        state.penaltyTypes.forEach { type ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = type.id in state.activePenaltyIds,
                    onCheckedChange = { viewModel.togglePenalty(type.id) },
                )
                Text("${type.emoji} ${type.label}", modifier = Modifier.weight(1f))
                if (type.isCustom) {
                    IconButton(onClick = { viewModel.deleteCustomPenalty(type.id) }) {
                        Icon(Icons.Default.Close, contentDescription = "삭제", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { showCustomDialog = true }) {
            Text("+ 커스텀 항목 추가")
        }
    }

    if (showCustomDialog) {
        CustomPenaltyDialog(
            onDismiss = { showCustomDialog = false },
            onAdd = { label, emoji ->
                viewModel.addCustomPenalty(label, emoji)
                showCustomDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowChips(labels: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { idx, label ->
            FilterChip(
                selected = idx == selectedIndex,
                onClick = { onSelect(idx) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun CustomPenaltyDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var label by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("커스텀 벌칙 추가") },
        text = {
            Column {
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it.take(2) },
                    label = { Text("이모지 (선택)") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(12) },
                    label = { Text("항목 이름") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(label, emoji) }, enabled = label.isNotBlank()) {
                Text("추가")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}
