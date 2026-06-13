package com.golfsupporter.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfsupporter.data.model.GameSession
import com.golfsupporter.data.repository.GameRepository
import com.golfsupporter.util.ScoreLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class HistoryItem(
    val sessionId: String,
    val dateText: String,
    val players: String,
    val rankingSummary: String,
    val courseName: String?,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    repository: GameRepository,
) : ViewModel() {

    private val dateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())

    val items = repository.observeCompletedSessions()
        .map { sessions -> sessions.map { it.toItem() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun GameSession.toItem(): HistoryItem {
        val standings = players.map { player ->
            val total = scores.filter { it.isConfirmed }.sumOf { it.playerScores[player.id] ?: 0 }
            player.name to total
        }.sortedBy { it.second }

        return HistoryItem(
            sessionId = id,
            dateText = dateFormat.format(Date(createdAt)),
            players = players.joinToString(" / ") { it.name },
            rankingSummary = standings.joinToString("  ") { "${it.first} ${ScoreLabel.formatTotal(it.second)}" },
            courseName = settings.courseName,
        )
    }
}
