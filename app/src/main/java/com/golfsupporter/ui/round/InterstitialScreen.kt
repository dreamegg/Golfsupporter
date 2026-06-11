package com.golfsupporter.ui.round

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun InterstitialScreen(
    onStartBackNine: (String) -> Unit,
    onLater: () -> Unit,
    viewModel: InterstitialViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val medals = listOf("🥇", "🥈", "🥉", "4️⃣")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Text("🏌️ 전반 완료 (Hole 1~9)", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Divider()
        Spacer(Modifier.height(16.dp))

        Text("현재 순위", fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        state.standings.forEachIndexed { index, standing ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(medals.getOrElse(index) { "" }, fontSize = 18.sp)
                Spacer(Modifier.width(12.dp))
                Text(standing.name, modifier = Modifier.weight(1f), fontSize = 16.sp)
                Text(standing.totalLabel, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        if (state.penaltyLines.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(12.dp))
            Text("전반 벌칙", fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            state.penaltyLines.forEach { line ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text("${line.playerName}: ", fontWeight = FontWeight.Medium)
                    Text(line.summary, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = { viewModel.startBackNine(onStartBackNine) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("지금 후반 시작", fontSize = 16.sp) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onLater,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("나중에 후반 하기", fontSize = 16.sp) }
    }
}
