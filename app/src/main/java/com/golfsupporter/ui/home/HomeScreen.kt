package com.golfsupporter.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.golfsupporter.data.model.RoundPhase

@Composable
fun HomeScreen(
    onNewGame: () -> Unit,
    onContinue: (String) -> Unit,
    onStartBack: (String) -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDiscard by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "⛳ Golf Score Tracker",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(32.dp))

        state.banner?.let { banner ->
            ContinueBanner(
                banner = banner,
                onPrimary = {
                    if (banner.phase == RoundPhase.FRONT_COMPLETED) onStartBack(banner.sessionId)
                    else onContinue(banner.sessionId)
                },
                onDiscard = { confirmDiscard = banner.sessionId },
            )
            Spacer(Modifier.height(24.dp))
        }

        Button(
            onClick = onNewGame,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text("새 게임 시작", fontSize = 18.sp)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onOpenHistory,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("지난 게임 보기", fontSize = 16.sp)
        }
    }

    confirmDiscard?.let { sessionId ->
        AlertDialog(
            onDismissRequest = { confirmDiscard = null },
            title = { Text("새로 시작할까요?") },
            text = { Text("진행 중인 라운드는 완료 처리되며 더 이상 이어서 진행할 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.discardActive(sessionId)
                    confirmDiscard = null
                    onNewGame()
                }) { Text("새로 시작") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = null }) { Text("취소") }
            }
        )
    }
}

@Composable
private fun ContinueBanner(
    banner: ActiveGameBanner,
    onPrimary: () -> Unit,
    onDiscard: () -> Unit,
) {
    val waitingBack = banner.phase == RoundPhase.FRONT_COMPLETED
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = if (waitingBack) "🏌️ 전반 완료, 후반 대기 중" else "⏸ 진행 중인 라운드",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (waitingBack) banner.playerNames
                else "${banner.playerNames} — Hole ${banner.currentHole}/${banner.lastHole}",
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = banner.standings.joinToString("  ") { "${it.name} ${it.totalLabel}" },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPrimary, modifier = Modifier.weight(1f)) {
                    Text(if (waitingBack) "▶ 후반 시작" else "▶ 이어하기")
                }
                OutlinedButton(onClick = onDiscard, modifier = Modifier.weight(1f)) {
                    Text("✕ 새로 시작")
                }
            }
        }
    }
}
