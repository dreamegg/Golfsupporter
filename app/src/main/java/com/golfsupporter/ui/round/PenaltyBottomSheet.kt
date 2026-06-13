package com.golfsupporter.ui.round

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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

/**
 * Penalty entry sheet (PRD F-022). Pick one or more players, then use the
 * explicit −/+ controls on each penalty type; the count applies to every
 * selected player and is saved immediately.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PenaltyBottomSheet(
    state: RoundUiState,
    onChange: (playerId: Int, typeId: String, delta: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Default selection: all players.
    var selected by remember { mutableStateOf(state.players.map { it.id }.toSet()) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            Text("벌칙 입력", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                "Hole ${state.currentHole} · 선택한 플레이어에게 −/+ 로 적용",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(12.dp))

            Text("플레이어 선택", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.players.forEach { player ->
                    FilterChip(
                        selected = player.id in selected,
                        onClick = {
                            selected = if (player.id in selected) selected - player.id
                            else selected + player.id
                        },
                        label = { Text(player.name) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("벌칙 항목", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            state.activePenaltyTypes.forEach { type ->
                val totalForType = selected.sumOf { pid -> state.penalties[pid]?.get(type.id) ?: 0 }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${type.emoji} ${type.label}",
                        modifier = Modifier.weight(1f),
                        fontSize = 15.sp,
                    )
                    OutlinedIconButton(
                        onClick = { selected.forEach { pid -> onChange(pid, type.id, -1) } },
                        enabled = selected.isNotEmpty() && totalForType > 0,
                        modifier = Modifier.size(40.dp),
                    ) { Text("−", fontSize = 20.sp) }
                    Text(
                        "$totalForType",
                        modifier = Modifier.width(36.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    OutlinedIconButton(
                        onClick = { selected.forEach { pid -> onChange(pid, type.id, 1) } },
                        enabled = selected.isNotEmpty(),
                        modifier = Modifier.size(40.dp),
                    ) { Text("+", fontSize = 20.sp) }
                }
                Divider()
            }

            Spacer(Modifier.height(20.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("완료")
            }
        }
    }
}
